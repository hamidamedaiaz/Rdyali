package com.softpath.riverpath.meshconverter;

import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.ProgressReporter;
import com.softpath.riverpath.util.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.softpath.riverpath.util.UtilityClass.buildTExtentionName;

/**
 * Factory for creating mesh converters
 * Can auto-detect dimension from file or use explicit dimension
 */
@Slf4j
public class MeshConverterFactory {

    private MeshConverterFactory() {
        // Prevent instantiation
    }

    /**
     * Convenience method: auto-detect and convert in one call
     *
     * @param inputPath path to .msh file
     * @return ouput fileName
     */
    public static String convert(String inputPath) {
        int dimension;
        if (DomainProperties.getInstance().isDimensionInitiliazed()) {
            dimension = DomainProperties.getInstance().getDimension();
        } else {
            dimension = detectDimension(inputPath);
        }
        MeshConverter converter = createFromFile(dimension, Runtime.getRuntime().availableProcessors());
        String outputFileName = buildTExtentionName(new File(inputPath));
        ProgressReporter.report("Converting GMSH file : " + inputPath);
        File outputFile = new File(UtilityClass.workspaceDirectory, outputFileName);
        converter.convert(inputPath, outputFile.getAbsolutePath());
        return outputFileName;
    }

    /**
     * Detect mesh dimension from file
     * <p>
     * Logic: If any node has z != 0 -> 3D, otherwise 2D
     *
     * @param filePath path to .msh file
     * @return 2 or 3
     */
    public static int detectDimension(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;

            // Find $Nodes section
            while ((line = reader.readLine()) != null) {
                if (line.replace("\r", "").trim().equals("$Nodes")) {
                    break;
                }
            }

            // Read through nodes
            while ((line = reader.readLine()) != null) {
                line = line.replace("\r", "").trim();
                if (line.equals("$EndNodes")) break;
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");

                // Node coordinate line has 3 parts: x y z
                if (parts.length == 3) {
                    try {
                        double z = Double.parseDouble(parts[2]);
                        if (z != 0.0) {
                            return 3; // Early exit
                        }
                    } catch (NumberFormatException ex) {
                        log.error("Error while parsing double", ex);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return 2; // No z != 0 found
    }

    /**
     * Create converter by auto-detecting dimension from file with custom thread count
     *
     * @param dimension  Domain dimension
     * @param numThreads number of threads to use
     * @return appropriate MeshConverter
     */
    private static MeshConverter createFromFile(int dimension, int numThreads) {
        return create(dimension, numThreads);
    }

    /**
     * Create converter for specified dimension with custom thread count
     *
     * @param dimension  2 or 3
     * @param numThreads number of threads to use
     * @return appropriate MeshConverter
     */
    private static MeshConverter create(int dimension, int numThreads) {
        return switch (dimension) {
            case 2 -> new MeshConverter2D(numThreads);
            case 3 -> new MeshConverter3D(numThreads);
            default ->
                    throw new IllegalArgumentException("Unsupported dimension: " + dimension + ". Only 2D and 3D are supported.");
        };
    }

}
