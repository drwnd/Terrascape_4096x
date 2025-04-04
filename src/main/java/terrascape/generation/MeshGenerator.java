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

        ArrayList<Integer> waterVerticesList = new ArrayList<>();
        @SuppressWarnings("unchecked") ArrayList<Integer>[] vertexLists = new ArrayList[OpaqueModel.FACE_TYPE_COUNT];
        for (int index = 0; index < vertexLists.length; index++) vertexLists[index] = new ArrayList<>();

        // Cache all materials in the chunk
        for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX++)
            for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ++)
                for (int inChunkY = 0; inChunkY < CHUNK_SIZE; inChunkY++)
                    materials[inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY] = chunk.getSaveMaterial(inChunkX, inChunkY, inChunkZ);

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
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                lower[materialX << CHUNK_SIZE_BITS | materialY] = materials[materialX << CHUNK_SIZE_BITS * 2 | materialY];

        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            // Fill materials
            if (materialZ == CHUNK_SIZE - 1) for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                    upper[materialX << CHUNK_SIZE_BITS | materialY] = chunk.getMaterial(materialX, materialY, CHUNK_SIZE);
            else for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                    upper[materialX << CHUNK_SIZE_BITS | materialY] = materials[materialX << CHUNK_SIZE_BITS * 2 | materialZ + 1 << CHUNK_SIZE_BITS | materialY];

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
                if ((toMeshFacesMap[materialX] & 1L << materialY) == 0) continue;

                int index = materialX << CHUNK_SIZE_BITS | materialY;
                byte material = toMesh[index];
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndY = materialY + 1, faceEndX = materialX + 1;

                // Grow face Y
                for (; faceEndY < CHUNK_SIZE; faceEndY++) {
                    index = materialX << CHUNK_SIZE_BITS | faceEndY;
                    if ((toMeshFacesMap[materialX] & 1L << faceEndY) == 0 || toMesh[index] != material) break;
                }
                long mask = faceEndY - materialY == CHUNK_SIZE ? -1L : (1L << faceEndY - materialY) - 1 << materialY;
                faceEndY--; // Account for increment then checks

                // Grow face X
                x_expansion:
                for (; faceEndX < CHUNK_SIZE && (toMeshFacesMap[faceEndX] & mask) == mask; faceEndX++)
                    for (int y = materialY; y <= faceEndY; y++)
                        if ((toMesh[faceEndX << CHUNK_SIZE_BITS | y] != material)) break x_expansion;
                faceEndX--; // Account for increment then checks

                // Remove face from bitmap
                mask = ~mask;
                for (int x = materialX; x <= faceEndX; x++) toMeshFacesMap[x] &= mask;

                // Add face
                if (Material.isWaterMaterial(material))
                    addFace(waterVertices, side, materialX, materialY, materialZ, u, v, faceEndY - materialY, faceEndX - materialX);
                else
                    addFace(opaqueVertices, side, materialX, materialY, materialZ, u, v, faceEndY - materialY, faceEndX - materialX);
            }
    }


    private void addTopBottomFaces(ArrayList<Integer>[] opaqueVertices, ArrayList<Integer> waterVertices) {
        // Fill materials
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                lower[materialX << CHUNK_SIZE_BITS | materialZ] = materials[materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS];

        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            // Fill materials
            if (materialY == CHUNK_SIZE - 1) for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                    upper[materialX << CHUNK_SIZE_BITS | materialZ] = chunk.getMaterial(materialX, CHUNK_SIZE, materialZ);
            else for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
                for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                    upper[materialX << CHUNK_SIZE_BITS | materialZ] = materials[materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS | materialY + 1];

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
                if ((toMeshFacesMap[materialX] & 1L << materialZ) == 0) continue;

                int index = materialX << CHUNK_SIZE_BITS | materialZ;
                byte material = toMesh[index];
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndX = materialX + 1, faceEndZ = materialZ + 1;

                // Grow face Z
                for (; faceEndZ < CHUNK_SIZE; faceEndZ++) {
                    index = materialX << CHUNK_SIZE_BITS | faceEndZ;
                    if ((toMeshFacesMap[materialX] & 1L << faceEndZ) == 0 || toMesh[index] != material) break;
                }
                long mask = faceEndZ - materialZ == CHUNK_SIZE ? -1L : (1L << faceEndZ - materialZ) - 1 << materialZ;
                faceEndZ--; // Account for increment then checks

                // Grow face X
                x_expansion:
                for (; faceEndX < CHUNK_SIZE && (toMeshFacesMap[faceEndX] & mask) == mask; faceEndX++)
                    for (int z = materialZ; z <= faceEndZ; z++)
                        if (toMesh[faceEndX << CHUNK_SIZE_BITS | z] != material) break x_expansion;
                faceEndX--; // Account for increment then checks

                // Remove face from bitmap
                mask = ~mask;
                for (int x = materialX; x <= faceEndX; x++) toMeshFacesMap[x] &= mask;

                // Add face
                if (Material.isWaterMaterial(material))
                    addFace(waterVertices, side, materialX, materialY, materialZ, u, v, faceEndX - materialX, faceEndZ - materialZ);
                else
                    addFace(opaqueVertices, side, materialX, materialY, materialZ, u, v, faceEndX - materialX, faceEndZ - materialZ);
            }
    }


    private void addWestEastFaces(ArrayList<Integer>[] opaqueVertices, ArrayList<Integer> waterVertices) {
        // Fill materials
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                lower[materialZ << CHUNK_SIZE_BITS | materialY] = materials[materialZ << CHUNK_SIZE_BITS | materialY];

        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            // Fill materials
            if (materialX == CHUNK_SIZE - 1) for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                    upper[materialZ << CHUNK_SIZE_BITS | materialY] = chunk.getMaterial(CHUNK_SIZE, materialY, materialZ);
            else for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                    upper[materialZ << CHUNK_SIZE_BITS | materialY] = materials[materialX + 1 << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS | materialY];

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
                if ((toMeshFacesMap[materialZ] & 1L << materialY) == 0) continue;

                int index = materialZ << CHUNK_SIZE_BITS | materialY;
                byte material = toMesh[index];
                int texture = Material.getTextureIndex(material);
                int u = texture & 15;
                int v = texture >> 4 & 15;
                int faceEndY = materialY + 1, faceEndZ = materialZ + 1;

                // Grow face Y
                for (; faceEndY < CHUNK_SIZE; faceEndY++) {
                    index = materialZ << CHUNK_SIZE_BITS | faceEndY;
                    if ((toMeshFacesMap[materialZ] & 1L << faceEndY) == 0 || toMesh[index] != material) break;
                }
                long mask = faceEndY - materialY == CHUNK_SIZE ? -1L : (1L << faceEndY - materialY) - 1 << materialY;
                faceEndY--; // Account for increment then checks

                // Grow face Z
                z_expansion:
                for (; faceEndZ < CHUNK_SIZE && (toMeshFacesMap[faceEndZ] & mask) == mask; faceEndZ++)
                    for (int y = materialY; y <= faceEndY; y++)
                        if (toMesh[faceEndZ << CHUNK_SIZE_BITS | y] != material) break z_expansion;
                faceEndZ--; // Account for increment then checks

                // Remove face from bitmap
                mask = ~mask;
                for (int z = materialZ; z <= faceEndZ; z++) toMeshFacesMap[z] &= mask;

                // Add face
                if (Material.isWaterMaterial(material))
                    addFace(waterVertices, side, materialX, materialY, materialZ, u, v, faceEndY - materialY, faceEndZ - materialZ);
                else
                    addFace(opaqueVertices, side, materialX, materialY, materialZ, u, v, faceEndY - materialY, faceEndZ - materialZ);
            }
    }

    private static void addFace(ArrayList<Integer> vertices, int side, int materialX, int materialY, int materialZ, int u, int v, int faceSize1, int faceSize2) {
        vertices.add(faceSize1 << 24 | faceSize2 << 18 | materialX << 12 | materialY << 6 | materialZ);
        vertices.add(side << 8 | u << 4 | v);
    }

    private static boolean occludes(byte toTestMaterial, byte occludingMaterial) {
        // Water occlusion
        if (Material.isWaterMaterial(toTestMaterial))
            return Material.isWaterMaterial(occludingMaterial) || (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;

        // Opaque occlusion
        if (toTestMaterial == LAVA)
            return occludingMaterial == LAVA || (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;
        if (Material.isGlassMaterial(toTestMaterial)) return Material.isGlassMaterial(occludingMaterial);
        return (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;
    }

    private Chunk chunk;
    private final long[] toMeshFacesMap = new long[CHUNK_SIZE];
    private byte[] upper = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private byte[] lower = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private final byte[] materials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
}
