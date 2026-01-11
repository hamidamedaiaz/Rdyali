package com.softpath.riverpath.meshconverter;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * 3D Mesh Converter
 * Handles tetrahedral meshes with triangular face boundaries
 */
public class MeshConverter3D extends AbstractMeshConverter {

    public MeshConverter3D(int numThreads) {
        super(numThreads);
    }

    @Override
    public int getDimension() {
        return 3;
    }

    @Override
    protected int getNodesPerMainElement() {
        return 4; // Tetrahedron
    }

    @Override
    protected int getNodesPerBoundaryElement() {
        return 3; // Triangle face
    }

    @Override
    protected int getElementType() {
        return 4;
    }

    @Override
    protected String getMainElementName() {
        return "Tetrahedra";
    }

    @Override
    protected String getBoundaryElementName() {
        return "faces";
    }

    @Override
    protected void parseElementsParallel() {
        parseElements();
    }

    @Override
    protected void detectBoundaryElements() {
        int numTets = mainElements.length;

        // Extract all faces (4 per tetrahedron)
        // Face ordering is CRITICAL for correct normals
        int[][] allFaces = new int[numTets * 4][3];

        try {
            pool.submit(() -> {
                IntStream.range(0, numTets).parallel().forEach(i -> {
                    int[] tet = mainElements[i];
                    allFaces[i * 4] = new int[]{tet[0], tet[2], tet[1]};
                    allFaces[i * 4 + 1] = new int[]{tet[0], tet[1], tet[3]};
                    allFaces[i * 4 + 2] = new int[]{tet[0], tet[3], tet[2]};
                    allFaces[i * 4 + 3] = new int[]{tet[1], tet[2], tet[3]};
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Face extraction failed", e);
        }

        // Count faces
        ConcurrentHashMap<FaceKey, FaceData> faceMap = new ConcurrentHashMap<>();

        try {
            pool.submit(() -> {
                Arrays.stream(allFaces).parallel().forEach(face -> {
                    FaceKey key = new FaceKey(face);
                    faceMap.compute(key, (k, v) -> {
                        if (v == null) {
                            return new FaceData(face, 1);
                        } else {
                            v.count++;
                            return v;
                        }
                    });
                });
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Face counting failed", e);
        }

        // Filter boundary faces (count == 1)
        boundaryElements = faceMap.values().stream()
                .filter(fd -> fd.count == 1)
                .map(fd -> fd.originalFace)
                .toArray(int[][]::new);
    }

    @Override
    protected String formatNode(double[] node) {
        return String.format(Locale.US, "%.16f %.16f %.16f %n", node[0], node[1], node[2]);
    }

    @Override
    protected String formatMainElement(int[] element) {
        return String.format(Locale.US, "%d %d %d %d %n", element[0], element[1], element[2], element[3]);
    }

    @Override
    protected String formatBoundaryElement(int[] element) {
        return String.format(Locale.US, "%d %d %d 0 %n", element[0], element[1], element[2]);
    }

    // ========== Helper classes ==========

    private static class FaceKey {
        private final int a, b, c;

        FaceKey(int[] face) {
            int x = face[0], y = face[1], z = face[2];
            if (x > y) {
                int t = x;
                x = y;
                y = t;
            }
            if (y > z) {
                int t = y;
                y = z;
                z = t;
            }
            if (x > y) {
                int t = x;
                x = y;
                y = t;
            }
            this.a = x;
            this.b = y;
            this.c = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FaceKey that)) return false;
            return a == that.a && b == that.b && c == that.c;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * a + b) + c;
        }
    }

    private static class FaceData {
        int[] originalFace;
        int count;

        FaceData(int[] face, int count) {
            this.originalFace = face;
            this.count = count;
        }
    }
}
