package com.softpath.riverpath.meshconverter;

import com.softpath.riverpath.util.ProgressReporter;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;


/**
 * 2D Mesh Converter
 * Handles triangular meshes with edge boundaries
 */
public class MeshConverter2D extends AbstractMeshConverter {

    public MeshConverter2D(int numThreads) {
        super(numThreads);
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    protected int getNodesPerMainElement() {
        return 3; // Triangle
    }

    @Override
    protected int getNodesPerBoundaryElement() {
        return 2; // Edge
    }

    @Override
    protected int getElementType() {
        return 2;
    }

    @Override
    protected String getMainElementName() {
        return "Triangles";
    }

    @Override
    protected String getBoundaryElementName() {
        return "edges";
    }

    @Override
    protected void parseElementsParallel() {
        parseElements();
        // Fix normals after parsing
        checkAndFixNormals();
    }

    /**
     * Check and fix 2D triangle normals (must be CW for Cimlib)
     */
    private void checkAndFixNormals() {
        if (mainElements.length == 0) return;

        int[] tri = mainElements[0];
        double[] v0 = nodes[tri[0] - 1];
        double[] v1 = nodes[tri[1] - 1];
        double[] v2 = nodes[tri[2] - 1];

        // 2D cross product (z component)
        double e1x = v1[0] - v0[0];
        double e1y = v1[1] - v0[1];
        double e2x = v2[0] - v0[0];
        double e2y = v2[1] - v0[1];
        double normal = e1x * e2y - e1y * e2x;

        if (normal > 0) {
            ProgressReporter.report("        Flipping triangle normals (CCW -> CW)");
            try {
                pool.submit(() -> {
                    Arrays.stream(mainElements).parallel().forEach(t -> {
                        int temp = t[1];
                        t[1] = t[2];
                        t[2] = temp;
                    });
                }).get();
            } catch (Exception e) {
                throw new RuntimeException("Triangle normal flip failed", e);
            }
        }
    }

    @Override
    protected void detectBoundaryElements() {
        int numTris = mainElements.length;

        // Extract all edges (3 per triangle)
        int[][] allEdges = new int[numTris * 3][2];

        try {
            pool.submit(() -> {
                IntStream.range(0, numTris).parallel().forEach(i -> {
                    int[] tri = mainElements[i];
                    allEdges[i * 3] = new int[]{tri[0], tri[1]};
                    allEdges[i * 3 + 1] = new int[]{tri[1], tri[2]};
                    allEdges[i * 3 + 2] = new int[]{tri[2], tri[0]};
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Edge extraction failed", e);
        }

        // Count edges
        ConcurrentHashMap<EdgeKey, EdgeData> edgeMap = new ConcurrentHashMap<>();

        try {
            pool.submit(() -> {
                Arrays.stream(allEdges).parallel().forEach(edge -> {
                    EdgeKey key = new EdgeKey(edge);
                    edgeMap.compute(key, (k, v) -> {
                        if (v == null) {
                            return new EdgeData(edge, 1);
                        } else {
                            v.count++;
                            return v;
                        }
                    });
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Edge counting failed", e);
        }

        // Filter boundary edges (count == 1)
        boundaryElements = edgeMap.values().stream()
                .filter(ed -> ed.count == 1)
                .map(ed -> ed.originalEdge)
                .toArray(int[][]::new);
    }

    @Override
    protected String formatNode(double[] node) {
        return String.format(Locale.US, "%.16f %.16f %n", node[0], node[1]);
    }

    @Override
    protected String formatMainElement(int[] element) {
        return String.format(Locale.US, "%d %d %d %n", element[0], element[1], element[2]);
    }

    @Override
    protected String formatBoundaryElement(int[] element) {
        return String.format(Locale.US, "%d %d 0 %n", element[0], element[1]);
    }

    // ========== Helper classes ==========

    private static class EdgeKey {
        private final int a, b;

        EdgeKey(int[] edge) {
            if (edge[0] < edge[1]) {
                this.a = edge[0];
                this.b = edge[1];
            } else {
                this.a = edge[1];
                this.b = edge[0];
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EdgeKey that)) return false;
            return a == that.a && b == that.b;
        }

        @Override
        public int hashCode() {
            return 31 * a + b;
        }
    }

    private static class EdgeData {
        int[] originalEdge;
        int count;

        EdgeData(int[] edge, int count) {
            this.originalEdge = edge;
            this.count = count;
        }
    }
}
