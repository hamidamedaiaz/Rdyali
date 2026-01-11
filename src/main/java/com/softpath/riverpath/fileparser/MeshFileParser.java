package com.softpath.riverpath.fileparser;

import com.softpath.riverpath.util.DomainProperties;
import com.softpath.riverpath.util.ProgressReporter;
import javafx.geometry.Point3D;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static com.softpath.riverpath.util.UtilityClass.buildMessage;

/**
 * Class to parse .t and transform the content to MeshStructure object
 *
 * @author rhajou
 */
public class MeshFileParser {

    /**
     * Parse a .t file and return a MeshStructure object
     *
     * @param file the file to parse
     * @return a MeshStructure object
     */
    public static MeshResolution parseFile2TriangleMesh(File file) {
        long startTime = System.currentTimeMillis();
        ProgressReporter.report("Start loading .t file at ");
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 1024 * 1024)) { // 1MB buffer
            // Read header
            String[] header = reader.readLine().split(" ");
            int numberOfPoints = Integer.parseInt(header[0]);
            int dimension = Integer.parseInt(header[1]);
            int nbTetra = Integer.parseInt(header[2]);

            MeshResolution meshResolution = new MeshResolution(nbTetra);
            boolean is3D = dimension == 3;

            // Read points
            for (int i = 0; i < numberOfPoints; i++) {
                String line = reader.readLine();
                int idx1 = line.indexOf(' ');
                if (is3D) {
                    int idx2 = line.indexOf(' ', idx1 + 1);
                    double x = Double.parseDouble(line.substring(0, idx1));
                    double y = Double.parseDouble(line.substring(idx1 + 1, idx2));
                    double z = Double.parseDouble(line.substring(idx2 + 1));
                    meshResolution.addPoint(new Point3D(x, y, z));
                } else {
                    double x = Double.parseDouble(line.substring(0, idx1));
                    double y = Double.parseDouble(line.substring(idx1 + 1));
                    meshResolution.addPoint(new Point3D(x, y, 0));
                }
            }
            // handle faces of triangle mesh
            handleFaces(reader, meshResolution);
            // add texture coordinates at the end
            meshResolution.addTexCoords();
            long elapsed = System.currentTimeMillis() - startTime;
            ProgressReporter.report(buildMessage("End Loading .t file %d ms%n", elapsed));
            return meshResolution;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleFaces(BufferedReader  reader, MeshResolution meshResolution) throws IOException {
        // Read faces/tetrahedra
        String line;
        while ((line = reader.readLine()) != null) {
            // Split the line into id1, id2 and id3
            String[] coordinates = line.split(StringUtils.SPACE);
            if (DomainProperties.getInstance().is3D()) {
                parseFaces3D(coordinates, meshResolution);
            } else {
                // 2D case : no need to handle resolution for the moment
                parseFaces2D(coordinates, meshResolution);
            }
        }
    }

    private static void parseFaces2D(String[] coordinates, MeshResolution meshResolution) {
        {
            int v1 = Integer.parseInt(coordinates[0]) - 1;
            int v2 = Integer.parseInt(coordinates[1]) - 1;
            int v3 = Integer.parseInt(coordinates[2]) - 1;
            meshResolution.add2DTriangle(v1, v2, v3);
        }
    }

    private static void parseFaces3D(String[] coordinates, MeshResolution meshResolution) {
        {
            int v1 = Integer.parseInt(coordinates[0]) - 1;
            int v2 = Integer.parseInt(coordinates[1]) - 1;
            int v3 = Integer.parseInt(coordinates[2]) - 1;
            int last = Integer.parseInt(coordinates[3]) - 1;
            if (last == -1) {
                // all surfaces for both resolutions
                meshResolution.addTriangleSurface(v1, v2, v3);
            } else {
                meshResolution.addTetraFaces(v1, v2, v3, last);
            }
        }
    }
}
