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
//
        chunk.generateSurroundingChunks();
        chunk.generateOcclusionCullingData();

        long[] bitmap = new long[CHUNK_SIZE * CHUNK_SIZE >> 6];
        ArrayList<Integer> waterVerticesList = new ArrayList<>();
        @SuppressWarnings("unchecked") ArrayList<Integer>[] vertexLists = new ArrayList[OpaqueModel.FACE_TYPE_COUNT];
        for (int index = 0; index < vertexLists.length; index++) vertexLists[index] = new ArrayList<>();

        addNorthSouthFaces(NORTH, bitmap, vertexLists[NORTH], waterVerticesList);
        addTopBottomFaces(TOP, bitmap, vertexLists[TOP], waterVerticesList);
        addWestEastFaces(WEST, bitmap, vertexLists[WEST], waterVerticesList);
        addNorthSouthFaces(SOUTH, bitmap, vertexLists[SOUTH], waterVerticesList);
        addTopBottomFaces(BOTTOM, bitmap, vertexLists[BOTTOM], waterVerticesList);
        addWestEastFaces(EAST, bitmap, vertexLists[EAST], waterVerticesList);

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


    private void addNorthSouthFaces(int side, long[] bitmap, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices) {
        if (side == NORTH) for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            addNorthSouthLayer(NORTH, bitmap, opaqueVertices, waterVertices, materialZ, 1);
        }
        else for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            addNorthSouthLayer(SOUTH, bitmap, opaqueVertices, waterVertices, materialZ, -1);
        }
    }

    private void addNorthSouthLayer(int side, long[] bitmap, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices, int materialZ, int normal) {
        // Fill bitmap
        int materialX;
        int materialY;
        for (materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                byte toTestMaterial = chunk.getSaveMaterial(materialX, materialY, materialZ);
                if (toTestMaterial == AIR) continue;
                byte occludingMaterial = chunk.getMaterial(materialX, materialY, materialZ + normal);
                if (occludes(toTestMaterial, occludingMaterial)) continue;

                int index = materialX << CHUNK_SIZE_BITS | materialY;
                bitmap[index >> 6] |= 1L << (index & 63);
            }

        // Generate faces
        for (materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                int index = materialX << CHUNK_SIZE_BITS | materialY;
                if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) continue;

                byte material = chunk.getSaveMaterial(materialX, materialY, materialZ);
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndY = materialY + 1, faceEndX = materialX + 1;

                // Grow face Y
                for (; faceEndY < CHUNK_SIZE; faceEndY++) {
                    index = materialX << CHUNK_SIZE_BITS | faceEndY;
                    if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) break;
                    if (chunk.getMaterial(materialX, faceEndY, materialZ) != material) break;
                }
                faceEndY--; // Account for increment then checks

                // Grow face X
                x_expansion:
                for (; faceEndX < CHUNK_SIZE; faceEndX++)
                    for (int y = materialY; y <= faceEndY; y++) {
                        index = faceEndX << CHUNK_SIZE_BITS | y;
                        if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) break x_expansion;
                        if (chunk.getSaveMaterial(faceEndX, y, materialZ) != material) break x_expansion;
                    }
                faceEndX--; // Account for increment then checks

                // Remove face from bitmap
                for (int x = materialX; x <= faceEndX; x++)
                    for (int y = materialY; y <= faceEndY; y++) {
                        index = x << CHUNK_SIZE_BITS | y;
                        bitmap[index >> 6] &= ~(1L << (index & 63));
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

    private void addTopBottomFaces(int side, long[] bitmap, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices) {
        if (side == TOP) for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            addTopBottomLayer(TOP, bitmap, opaqueVertices, waterVertices, materialY, 1);
        }
        else for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            addTopBottomLayer(BOTTOM, bitmap, opaqueVertices, waterVertices, materialY, -1);
        }
    }

    private void addTopBottomLayer(int side, long[] bitmap, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices, int materialY, int normal) {
        // Fill bitmap
        int materialX;
        int materialZ;
        for (materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
                byte toTestMaterial = chunk.getSaveMaterial(materialX, materialY, materialZ);
                if (toTestMaterial == AIR) continue;
                byte occludingMaterial = chunk.getMaterial(materialX, materialY + normal, materialZ);
                if (occludes(toTestMaterial, occludingMaterial)) continue;

                int index = materialX << CHUNK_SIZE_BITS | materialZ;
                bitmap[index >> 6] |= 1L << (index & 63);
            }

        // Generate faces
        for (materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
                int index = materialX << CHUNK_SIZE_BITS | materialZ;
                if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) continue;

                byte material = chunk.getSaveMaterial(materialX, materialY, materialZ);
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndX = materialX + 1, faceEndZ = materialZ + 1;

                // Grow face X
                for (; faceEndX < CHUNK_SIZE; faceEndX++) {
                    index = faceEndX << CHUNK_SIZE_BITS | materialZ;
                    if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) break;
                    if (chunk.getSaveMaterial(faceEndX, materialY, materialZ) != material) break;
                }
                faceEndX--; // Account for increment then checks

                // Grow face Z
                z_expansion:
                for (; faceEndZ < CHUNK_SIZE; faceEndZ++)
                    for (int x = materialX; x <= faceEndX; x++) {
                        index = x << CHUNK_SIZE_BITS | faceEndZ;
                        if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) break z_expansion;
                        if (chunk.getSaveMaterial(x, materialY, faceEndZ) != material) break z_expansion;
                    }
                faceEndZ--; // Account for increment then checks

                // Remove face from bitmap
                for (int x = materialX; x <= faceEndX; x++)
                    for (int z = materialZ; z <= faceEndZ; z++) {
                        index = x << CHUNK_SIZE_BITS | z;
                        bitmap[index >> 6] &= ~(1L << (index & 63));
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

    private void addWestEastFaces(int side, long[] bitmap, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices) {
        if (side == WEST) for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            addWestEastLayer(WEST, bitmap, opaqueVertices, waterVertices, materialX, 1);
        }
        else for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            addWestEastLayer(EAST, bitmap, opaqueVertices, waterVertices, materialX, -1);
        }
    }

    private void addWestEastLayer(int side, long[] bitmap, ArrayList<Integer> opaqueVertices, ArrayList<Integer> waterVertices, int materialX, int normal) {
        // Fill bitmap
        int materialZ;
        int materialY;
        for (materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                byte toTestMaterial = chunk.getSaveMaterial(materialX, materialY, materialZ);
                if (toTestMaterial == AIR) continue;
                byte occludingMaterial = chunk.getMaterial(materialX + normal, materialY, materialZ);
                if (occludes(toTestMaterial, occludingMaterial)) continue;

                int index = materialZ << CHUNK_SIZE_BITS | materialY;
                bitmap[index >> 6] |= 1L << (index & 63);
            }

        // Generate faces
        for (materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                int index = materialZ << CHUNK_SIZE_BITS | materialY;
                if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) continue;

                byte material = chunk.getSaveMaterial(materialX, materialY, materialZ);
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndY = materialY + 1, faceEndZ = materialZ + 1;

                // Grow face Y
                for (; faceEndY < CHUNK_SIZE; faceEndY++) {
                    index = materialZ << CHUNK_SIZE_BITS | faceEndY;
                    if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) break;
                    if (chunk.getSaveMaterial(materialX, faceEndY, materialZ) != material) break;
                }
                faceEndY--; // Account for increment then checks

                // Grow face Z
                z_expansion:
                for (; faceEndZ < CHUNK_SIZE; faceEndZ++)
                    for (int y = materialY; y <= faceEndY; y++) {
                        index = faceEndZ << CHUNK_SIZE_BITS | y;
                        if ((bitmap[index >> 6] & 1L << (index & 63)) == 0) break z_expansion;
                        if (chunk.getSaveMaterial(materialX, y, faceEndZ) != material) break z_expansion;
                    }
                faceEndZ--; // Account for increment then checks

                // Remove face from bitmap
                for (int z = materialZ; z <= faceEndZ; z++)
                    for (int y = materialY; y <= faceEndY; y++) {
                        index = z << CHUNK_SIZE_BITS | y;
                        bitmap[index >> 6] &= ~(1L << (index & 63));
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
}
