package terrascape.generation;

import terrascape.entity.OpaqueModel;
import terrascape.server.Material;
import terrascape.dataStorage.octree.Chunk;

import java.util.ArrayList;

import static terrascape.utils.Constants.*;

public final class MeshGenerator {

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public void generateMesh() {
        chunk.setMeshed(true);
        chunk.generateSurroundingChunks();
        chunk.generateOcclusionCullingData();

        ArrayList<Integer> waterVerticesList = new ArrayList<>();
        @SuppressWarnings("unchecked") ArrayList<Integer>[] vertexLists = new ArrayList[OpaqueModel.FACE_TYPE_COUNT];
        for (int index = 0; index < vertexLists.length; index++) vertexLists[index] = new ArrayList<>();

        addNorthSouthFaces(vertexLists, waterVerticesList);
        addTopBottomFaces(vertexLists, waterVerticesList);
        addWestEastFaces(vertexLists, waterVerticesList);

        int[] waterVertices = new int[waterVerticesList.size()];
        for (int i = 0, size = waterVerticesList.size(); i < size; i++) waterVertices[i] = waterVerticesList.get(i);
        chunk.setWaterVertices(waterVertices);

        int totalVertexCount = 0, verticesIndex = 0;
        for (ArrayList<Integer> vertexList : vertexLists) totalVertexCount += vertexList.size();
        int[] vertexCounts = new int[vertexLists.length];
        int[] opaqueVertices = new int[totalVertexCount];

        for (int index = 0; index < vertexLists.length; index++) {
            ArrayList<Integer> vertexList = vertexLists[index];
            vertexCounts[index] = vertexList.size() * 3;
            for (int vertex : vertexList) opaqueVertices[verticesIndex++] = vertex;
        }

        chunk.setOpaqueVertices(opaqueVertices);
        chunk.setVertexCounts(vertexCounts);
    }


    private void addNorthSouthFaces(ArrayList<Integer>[] opaqueVertices, ArrayList<Integer> waterVertices) {
        // Fill materials
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                int index = materialX << CHUNK_SIZE_BITS | materialY;
                lower[index] = chunk.getSaveMaterial(materialX, materialY, 0);
            }
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            // Fill materials
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                    int index = materialX << CHUNK_SIZE_BITS | materialY;
                    upper[index] = chunk.getMaterial(materialX, materialY, materialZ + 1);
                }

            addNorthSouthLayer(NORTH, opaqueVertices[NORTH], waterVertices, materialZ, lower, upper);
            addNorthSouthLayer(SOUTH, opaqueVertices[SOUTH], waterVertices, materialZ, upper, lower);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void addNorthSouthLayer(int side, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices, int materialZ, byte[] toMesh, byte[] occluding) {
        // Fill bitmap
        for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE; index++) {
            byte toTestMaterial = toMesh[index];
            if (toTestMaterial == AIR) continue;
            byte occludingMaterial = occluding[index];
            if (occludes(toTestMaterial, occludingMaterial)) continue;

            toMeshFacesMap[index >> 6] |= 1L << (index & 63);
        }

        // Generate faces
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                int index = materialX << CHUNK_SIZE_BITS | materialY;
                if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0) continue;

                byte material = toMesh[index];
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndY = materialY + 1, faceEndX = materialX + 1;

                // Grow face Y
                for (; faceEndY < CHUNK_SIZE; faceEndY++) {
                    index = materialX << CHUNK_SIZE_BITS | faceEndY;
                    if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0 || toMesh[index] != material) break;
                }
                faceEndY--; // Account for increment then checks

                // Grow face X
                x_expansion:
                for (; faceEndX < CHUNK_SIZE; faceEndX++)
                    for (int y = materialY; y <= faceEndY; y++) {
                        index = faceEndX << CHUNK_SIZE_BITS | y;
                        if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0 || toMesh[index] != material)
                            break x_expansion;
                    }
                faceEndX--; // Account for increment then checks

                // Remove face from bitmap
                for (int x = materialX; x <= faceEndX; x++)
                    for (int y = materialY; y <= faceEndY; y++) {
                        index = x << CHUNK_SIZE_BITS | y;
                        toMeshFacesMap[index >> 6] &= ~(1L << (index & 63));
                    }

                // Add face
                if (Material.isWaterMaterial(material)) {
                    waterVertices.add(side << 29 | u << 25 | v << 21 | materialX << 14 | materialY << 7 | materialZ);
                    waterVertices.add(faceEndY - materialY << 7 | faceEndX - materialX);
                } else {
                    opaqueVertices.add(side << 29 | u << 25 | v << 21 | materialX << 14 | materialY << 7 | materialZ);
                    opaqueVertices.add(faceEndY - materialY << 7 | faceEndX - materialX);
                }
            }
    }


    private void addTopBottomFaces(ArrayList<Integer>[] opaqueVertices, ArrayList<Integer> waterVertices) {
        // Fill materials
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
                int index = materialX << CHUNK_SIZE_BITS | materialZ;
                lower[index] = chunk.getSaveMaterial(materialX, 0, materialZ);
            }
        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            // Fill materials
            for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
                    int index = materialX << CHUNK_SIZE_BITS | materialZ;
                    upper[index] = chunk.getMaterial(materialX, materialY + 1, materialZ);
                }

            addTopBottomLayer(TOP, opaqueVertices[TOP], waterVertices, materialY, lower, upper);
            addTopBottomLayer(BOTTOM, opaqueVertices[BOTTOM], waterVertices, materialY, upper, lower);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void addTopBottomLayer(int side, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices, int materialY, byte[] toMesh, byte[] occluding) {
        // Fill bitmap
        for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE; index++) {
            byte toTestMaterial = toMesh[index];
            if (toTestMaterial == AIR) continue;
            byte occludingMaterial = occluding[index];
            if (occludes(toTestMaterial, occludingMaterial)) continue;

            toMeshFacesMap[index >> 6] |= 1L << (index & 63);
        }

        // Generate faces
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
                int index = materialX << CHUNK_SIZE_BITS | materialZ;
                if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0) continue;

                byte material = toMesh[index];
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndX = materialX + 1, faceEndZ = materialZ + 1;

                // Grow face X
                for (; faceEndX < CHUNK_SIZE; faceEndX++) {
                    index = faceEndX << CHUNK_SIZE_BITS | materialZ;
                    if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0 || toMesh[index] != material) break;
                }
                faceEndX--; // Account for increment then checks

                // Grow face Z
                z_expansion:
                for (; faceEndZ < CHUNK_SIZE; faceEndZ++)
                    for (int x = materialX; x <= faceEndX; x++) {
                        index = x << CHUNK_SIZE_BITS | faceEndZ;
                        if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0 || toMesh[index] != material)
                            break z_expansion;
                    }
                faceEndZ--; // Account for increment then checks

                // Remove face from bitmap
                for (int x = materialX; x <= faceEndX; x++)
                    for (int z = materialZ; z <= faceEndZ; z++) {
                        index = x << CHUNK_SIZE_BITS | z;
                        toMeshFacesMap[index >> 6] &= ~(1L << (index & 63));
                    }

                // Add face
                if (Material.isWaterMaterial(material)) {
                    waterVertices.add(side << 29 | u << 25 | v << 21 | materialX << 14 | materialY << 7 | materialZ);
                    waterVertices.add(faceEndX - materialX << 7 | faceEndZ - materialZ);
                } else {
                    opaqueVertices.add(side << 29 | u << 25 | v << 21 | materialX << 14 | materialY << 7 | materialZ);
                    opaqueVertices.add(faceEndX - materialX << 7 | faceEndZ - materialZ);
                }
            }
    }


    private void addWestEastFaces(ArrayList<Integer>[] opaqueVertices, ArrayList<Integer> waterVertices) {
        // Fill materials
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                int index = materialZ << CHUNK_SIZE_BITS | materialY;
                lower[index] = chunk.getSaveMaterial(0, materialY, materialZ);
            }
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            // Fill materials
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                    int index = materialZ << CHUNK_SIZE_BITS | materialY;
                    upper[index] = chunk.getMaterial(materialX + 1, materialY, materialZ);
                }

            addWestEastLayer(WEST, opaqueVertices[WEST], waterVertices, materialX, lower, upper);
            addWestEastLayer(EAST, opaqueVertices[EAST], waterVertices, materialX, upper, lower);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void addWestEastLayer(int side, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices, int materialX, byte[] toMesh, byte[] occluding) {
        // Fill bitmap
        for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE; index++) {
            byte toTestMaterial = toMesh[index];
            if (toTestMaterial == AIR) continue;
            byte occludingMaterial = occluding[index];
            if (occludes(toTestMaterial, occludingMaterial)) continue;

            toMeshFacesMap[index >> 6] |= 1L << (index & 63);
        }

        // Generate faces
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                int index = materialZ << CHUNK_SIZE_BITS | materialY;
                if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0) continue;

                byte material = toMesh[index];
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndY = materialY + 1, faceEndZ = materialZ + 1;

                // Grow face Y
                for (; faceEndY < CHUNK_SIZE; faceEndY++) {
                    index = materialZ << CHUNK_SIZE_BITS | faceEndY;
                    if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0 || toMesh[index] != material) break;
                }
                faceEndY--; // Account for increment then checks

                // Grow face Z
                z_expansion:
                for (; faceEndZ < CHUNK_SIZE; faceEndZ++)
                    for (int y = materialY; y <= faceEndY; y++) {
                        index = faceEndZ << CHUNK_SIZE_BITS | y;
                        if ((toMeshFacesMap[index >> 6] & 1L << (index & 63)) == 0 || toMesh[index] != material)
                            break z_expansion;
                    }
                faceEndZ--; // Account for increment then checks

                // Remove face from bitmap
                for (int z = materialZ; z <= faceEndZ; z++)
                    for (int y = materialY; y <= faceEndY; y++) {
                        index = z << CHUNK_SIZE_BITS | y;
                        toMeshFacesMap[index >> 6] &= ~(1L << (index & 63));
                    }

                // Add face
                if (Material.isWaterMaterial(material)) {
                    waterVertices.add(side << 29 | u << 25 | v << 21 | materialX << 14 | materialY << 7 | materialZ);
                    waterVertices.add(faceEndY - materialY << 7 | faceEndZ - materialZ);
                } else {
                    opaqueVertices.add(side << 29 | u << 25 | v << 21 | materialX << 14 | materialY << 7 | materialZ);
                    opaqueVertices.add(faceEndY - materialY << 7 | faceEndZ - materialZ);
                }
            }
    }


    private static boolean occludes(byte toTestMaterial, byte occludingMaterial) {
        // Water occlusion
        if (Material.isWaterMaterial(toTestMaterial))
            return Material.isWaterMaterial(occludingMaterial) || (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;

        // Opaque occlusion
        if (toTestMaterial == LAVA)
            return occludingMaterial == LAVA || (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;
        if (Material.isGlassType(toTestMaterial)) return Material.isGlassType(occludingMaterial);
        return (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;
    }

    private Chunk chunk;
    private final long[] toMeshFacesMap = new long[CHUNK_SIZE * CHUNK_SIZE >> 6];
    private byte[] upper = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private byte[] lower = new byte[CHUNK_SIZE * CHUNK_SIZE];

}
