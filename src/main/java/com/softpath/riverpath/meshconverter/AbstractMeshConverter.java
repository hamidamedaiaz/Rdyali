package com.softpath.riverpath.meshconverter;

import com.softpath.riverpath.util.ProgressReporter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static com.softpath.riverpath.util.UtilityClass.buildMessage;

/**
 * Abstract base class for mesh converters
 * Contains common functionality for file loading, node parsing, and output writing
 */
public abstract class AbstractMeshConverter implements MeshConverter {

    protected final int numThreads;
    protected final ForkJoinPool pool;

    // Raw file data
    protected String[] lines;

    // Section boundaries (line indices)
    protected int nodesStart, nodesEnd;
    protected int elementsStart, elementsEnd;

    // Parsed data
    protected double[][] nodes;
    protected int[][] mainElements;    // triangles (2D) or tetrahedra (3D)
    protected int[][] boundaryElements; // edges (2D) or faces (3D)

    // Mapping for node renumbering
    protected int[] oldToNewIndex;

    public AbstractMeshConverter(int numThreads) {
        this.numThreads = numThreads;
        this.pool = new ForkJoinPool(numThreads);
    }

    @Override
    public void convert(String inputPath, String outputPath) {
        long totalStart = System.currentTimeMillis();
        ProgressReporter.report(buildMessage("############################################################"));
        ProgressReporter.report(buildMessage("# GMSH to MTC CONVERTER - %dD Version                       #", getDimension()));
        ProgressReporter.report(buildMessage("#                  Using %d threads                        #", numThreads));
        ProgressReporter.report(buildMessage("############################################################"));

        // Step 1: Load file and locate sections
        long t0 = System.currentTimeMillis();
        loadFileAndLocateSections(inputPath);
        ProgressReporter.report(buildMessage("Step 1: File loaded & sections located: %d ms", System.currentTimeMillis() - t0));
        ProgressReporter.report(buildMessage("        Lines: %d", lines.length));

        // Step 2: Parse nodes in parallel
        t0 = System.currentTimeMillis();
        parseNodesParallel();
        ProgressReporter.report(buildMessage("Step 2: Nodes parsed (parallel): %d ms", System.currentTimeMillis() - t0));
        ProgressReporter.report(buildMessage("        Nodes: %d", nodes.length));

        // Step 3: Parse elements in parallel (dimension-specific)
        t0 = System.currentTimeMillis();
        parseElementsParallel();
        ProgressReporter.report(buildMessage("Step 3: Elements parsed (parallel): %d ms", System.currentTimeMillis() - t0));
        ProgressReporter.report(buildMessage("        %s: %d", getMainElementName(), mainElements.length));

        // Step 4: Detect boundary elements (dimension-specific)
        t0 = System.currentTimeMillis();
        detectBoundaryElements();
        ProgressReporter.report(buildMessage("Step 4: Boundary %s detected: %d ms", getBoundaryElementName(), System.currentTimeMillis() - t0));
        ProgressReporter.report(buildMessage("        Boundary %s: %d", getBoundaryElementName(), boundaryElements.length));

        // Step 5: Remove unused nodes and renumber
        t0 = System.currentTimeMillis();
        int removedCount = removeUnusedNodesParallel();
        ProgressReporter.report(buildMessage("Step 5: Unused nodes removed (parallel): %d ms", System.currentTimeMillis() - t0));
        ProgressReporter.report(buildMessage("        Removed: %d, Final nodes: %d", removedCount, nodes.length));

        // Step 6: Write output
        t0 = System.currentTimeMillis();
        writeOutputParallel(outputPath);
        ProgressReporter.report(buildMessage("Step 6: Output written (parallel): %d ms", System.currentTimeMillis() - t0));

        // Cleanup
        pool.shutdown();
        // To free memory immediately after conversion.
        // The lines array holds the entire file in memory (millions of strings for large meshes).
        // After conversion is done, we don't need it anymore.
        lines = null;

        ProgressReporter.report(buildMessage("Total time: %d ms", System.currentTimeMillis() - totalStart));
    }


// ========== Abstract methods (dimension-specific) ==========

    /**
     * Parse main elements (triangles for 2D, tetrahedra for 3D)
     */
    protected abstract void parseElementsParallel();

    /**
     * Detect boundary elements (edges for 2D, faces for 3D)
     */
    protected abstract void detectBoundaryElements();

    /**
     * Get number of nodes per main element (3 for triangle, 4 for tetrahedron)
     */
    protected abstract int getNodesPerMainElement();

    /**
     * Get number of nodes per boundary element (2 for edge, 3 for face)
     */
    protected abstract int getNodesPerBoundaryElement();

    /**
     * Get element type : 4 for 3D and 2 for 2D
     *
     * @return element type
     */
    protected abstract int getElementType();

    /**
     * Get main element name for logging
     */
    protected abstract String getMainElementName();

    /**
     * Get boundary element name for logging
     */
    protected abstract String getBoundaryElementName();

// ========== Common implementations ==========

    /**
     * Load file and locate section boundaries in single pass
     */
    protected void loadFileAndLocateSections(String path) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            lines = reader.lines().map(String::trim).toArray(String[]::new);

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                switch (line) {
                    case "$Nodes" -> nodesStart = i;
                    case "$EndNodes" -> nodesEnd = i;
                    case "$Elements" -> elementsStart = i;
                    case "$EndElements" -> elementsEnd = i;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse nodes from GMSH v4 format in parallel
     * <p>
     * GMSH V4 NODE SECTION FORMAT:
     * ============================
     * $Nodes
     * numEntityBlocks numNodes minNodeTag maxNodeTag    <- header line
     * entityDim entityTag parametric numNodesInBlock    <- block header
     * nodeTag1                                          <- node tags (skip)
     * nodeTag2
     * ...
     * x1 y1 z1                                          <- coordinates (parse)
     * x2 y2 z2
     * ...
     * entityDim entityTag parametric numNodesInBlock    <- next block header
     * ...
     * $EndNodes
     * <p>
     * ALGORITHM:
     * ==========
     * Phase 1 (Sequential): Scan to find coordinate line ranges
     * - For each block, record: [startLine, count, targetIndex]
     * - Skip tag lines, identify where coordinate lines are
     * <p>
     * Phase 2 (Parallel): Parse coordinates
     * - Each thread processes different blocks
     * - Write directly to pre-allocated nodes array
     */
    private void parseNodesParallel() {
        String[] header = lines[nodesStart + 1].split("\\s+");
        int totalNodes = Integer.parseInt(header[1]);
        nodes = new double[totalNodes][3];

        List<int[]> coordRanges = new ArrayList<>();
        int lineIdx = nodesStart + 2;
        int nodeTargetIdx = 0;

        while (lineIdx < nodesEnd) {
            String line = lines[lineIdx];
            if (line.isEmpty() || line.startsWith("$")) break;

            String[] blockHeader = line.split("\\s+");
            if (blockHeader.length < 4) break;

            int numNodesInBlock = Integer.parseInt(blockHeader[3]);
            lineIdx += 1 + numNodesInBlock;
            coordRanges.add(new int[]{lineIdx, numNodesInBlock, nodeTargetIdx});
            nodeTargetIdx += numNodesInBlock;
            lineIdx += numNodesInBlock;
        }

        try {
            pool.submit(() -> coordRanges.parallelStream().forEach(range -> {
                int startLine = range[0];
                int count = range[1];
                int targetIdx = range[2];

                for (int i = 0; i < count; i++) {
                    String[] coords = lines[startLine + i].split("\\s+");
                    nodes[targetIdx + i][0] = Double.parseDouble(coords[0]);
                    nodes[targetIdx + i][1] = Double.parseDouble(coords[1]);
                    nodes[targetIdx + i][2] = Double.parseDouble(coords[2]);
                }
            })).get();
        } catch (Exception e) {
            throw new RuntimeException("Parallel node parsing failed", e);
        }
    }

    /**
     * Remove unused nodes and renumber in parallel
     */
    protected int removeUnusedNodesParallel() {
        int maxNodeIndex = nodes.length;
        boolean[] used = new boolean[maxNodeIndex + 1];

        try {
            pool.submit(() -> {
                Arrays.stream(mainElements).parallel().forEach(elem -> {
                    for (int idx : elem) used[idx] = true;
                });
                Arrays.stream(boundaryElements).parallel().forEach(elem -> {
                    for (int idx : elem) used[idx] = true;
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Node marking failed", e);
        }

        oldToNewIndex = new int[maxNodeIndex + 1];
        int newCount = 0;
        for (int i = 1; i <= maxNodeIndex; i++) {
            if (used[i]) {
                oldToNewIndex[i] = ++newCount;
            }
        }

        int removedCount = maxNodeIndex - newCount;

        double[][] newNodes = new double[newCount][];
        try {
            pool.submit(() -> {
                IntStream.range(1, maxNodeIndex + 1)
                        .parallel()
                        .filter(i -> used[i])
                        .forEach(i -> {
                            int newIdx = oldToNewIndex[i] - 1;
                            newNodes[newIdx] = nodes[i - 1];
                        });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Node compaction failed", e);
        }
        nodes = newNodes;

        try {
            pool.submit(() -> {
                Arrays.stream(mainElements).parallel().forEach(elem -> {
                    for (int i = 0; i < elem.length; i++) {
                        elem[i] = oldToNewIndex[elem[i]];
                    }
                });
                Arrays.stream(boundaryElements).parallel().forEach(elem -> {
                    for (int i = 0; i < elem.length; i++) {
                        elem[i] = oldToNewIndex[elem[i]];
                    }
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Element renumbering failed", e);
        }

        return removedCount;
    }

    /**
     * Write output with parallel string building
     */
    protected void writeOutputParallel(String path) {
        int totalElements = mainElements.length + boundaryElements.length;
        int valuesPerLine = getDimension() + 1;

        // Build node strings in parallel
        String[] nodeStrings = new String[nodes.length];
        try {
            pool.submit(() -> {
                IntStream.range(0, nodes.length).parallel().forEach(i -> {
                    nodeStrings[i] = formatNode(nodes[i]);
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Node string building failed", e);
        }

        // Build main element strings
        String[] mainElemStrings = new String[mainElements.length];
        try {
            pool.submit(() -> {
                IntStream.range(0, mainElements.length).parallel().forEach(i -> {
                    mainElemStrings[i] = formatMainElement(mainElements[i]);
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Main element string building failed", e);
        }

        // Build boundary element strings
        String[] boundaryStrings = new String[boundaryElements.length];
        try {
            pool.submit(() -> {
                IntStream.range(0, boundaryElements.length).parallel().forEach(i -> {
                    boundaryStrings[i] = formatBoundaryElement(boundaryElements[i]);
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Boundary string building failed", e);
        }

        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path), 1024 * 1024)) {
            writer.write(String.format(Locale.US, "%d %d %d %d%n",
                    nodes.length, getDimension(), totalElements, valuesPerLine));
            for (String s : nodeStrings) writer.write(s);
            for (String s : mainElemStrings) writer.write(s);
            for (String s : boundaryStrings) writer.write(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void parseElements() {
        List<int[]> elementRanges = new ArrayList<>();
        int totalElement = 0;
        int lineIdx = elementsStart + 2;

        while (lineIdx < elementsEnd) {
            String line = lines[lineIdx];
            if (line.isEmpty() || line.startsWith("$")) break;
            String[] blockHeader = line.split("\\s+");
            if (blockHeader.length < 4) break;
            int elementType = Integer.parseInt(blockHeader[2]);
            int numElements = Integer.parseInt(blockHeader[3]);
            if (elementType == getElementType()) { // Triangle or Tetrahedron
                elementRanges.add(new int[]{lineIdx + 1, numElements, totalElement});
                totalElement += numElements;
            }
            lineIdx += 1 + numElements;
        }

        mainElements = new int[totalElement][getNodesPerMainElement()];

        try {
            pool.submit(() -> {
                elementRanges.parallelStream().forEach(range -> {
                    int startLine = range[0];
                    int count = range[1];
                    int targetIdx = range[2];

                    for (int i = 0; i < count; i++) {
                        String[] parts = lines[startLine + i].split("\\s+");
                        mainElements[targetIdx + i][0] = Integer.parseInt(parts[1]);
                        mainElements[targetIdx + i][1] = Integer.parseInt(parts[2]);
                        mainElements[targetIdx + i][2] = Integer.parseInt(parts[3]);
                        if (getDimension() == 3) {
                            mainElements[targetIdx + i][3] = Integer.parseInt(parts[4]);
                        }
                    }
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Parallel " + getMainElementName() + " parsing failed", e);
        }
    }

    /**
     * Format a node for output (dimension-specific)
     */
    protected abstract String formatNode(double[] node);

    /**
     * Format a main element for output
     */
    protected abstract String formatMainElement(int[] element);

    /**
     * Format a boundary element for output
     */
    protected abstract String formatBoundaryElement(int[] element);
}
