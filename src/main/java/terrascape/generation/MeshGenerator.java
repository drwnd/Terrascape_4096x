package terrascape.generation;

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

        waterVerticesList.clear();
        glassVerticesList.clear();
        for (ArrayList<Integer> list : opaqueVerticesLists) list.clear();

        // Cache all materials in the chunk
        for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX++)
            for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ++)
                for (int inChunkY = 0; inChunkY < CHUNK_SIZE; inChunkY++)
                    materials[inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY] = chunk.getSaveMaterial(inChunkX, inChunkY, inChunkZ);

        addNorthSouthFaces();
        addTopBottomFaces();
        addWestEastFaces();

        int[] transparentVertices = new int[waterVerticesList.size() + glassVerticesList.size()];
        for (int i = 0, size = waterVerticesList.size(); i < size; i++)
            transparentVertices[i] = waterVerticesList.get(i);
        for (int i = 0, size = glassVerticesList.size(); i < size; i++)
            transparentVertices[i + waterVerticesList.size()] = glassVerticesList.get(i);
        chunk.setTransparentVertices(transparentVertices, waterVerticesList.size(), glassVerticesList.size());

        int totalVertexCount = 0, verticesIndex = 0;
        for (ArrayList<Integer> vertexList : opaqueVerticesLists) totalVertexCount += vertexList.size();
        int[] vertexCounts = new int[opaqueVerticesLists.length];
        int[] opaqueVertices = new int[totalVertexCount];

        for (int index = 0; index < opaqueVerticesLists.length; index++) {
            ArrayList<Integer> vertexList = opaqueVerticesLists[index];
            vertexCounts[index] = vertexList.size() * 3;
            for (int vertex : vertexList) opaqueVertices[verticesIndex++] = vertex;
        }

        chunk.setOpaqueVertices(opaqueVertices);
        chunk.setVertexCounts(vertexCounts);
    }


    private void addNorthSouthFaces() {
        // Copy materials
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2, lower, materialX << CHUNK_SIZE_BITS, CHUNK_SIZE);

        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
            copyMaterialsNorthSouth(materialZ);
            addNorthSouthLayer(NORTH, materialZ, lower, upper);
            addNorthSouthLayer(SOUTH, materialZ, upper, lower);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialsNorthSouth(int materialZ) {
        if (materialZ == CHUNK_SIZE - 1) for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                upper[materialX << CHUNK_SIZE_BITS | materialY] = chunk.getMaterial(materialX, materialY, CHUNK_SIZE);
        else for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            System.arraycopy(materials, materialX << CHUNK_SIZE_BITS * 2 | materialZ + 1 << CHUNK_SIZE_BITS, upper, materialX << CHUNK_SIZE_BITS, CHUNK_SIZE);
    }

    private void addNorthSouthLayer(int side, int materialZ, byte[] toMesh, byte[] occluding) {
        fillToMeshFacesMap(toMesh, occluding);

        // Generate faces
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                if ((toMeshFacesMap[materialX] & 1L << materialY) == 0) continue;

                int index = materialX << CHUNK_SIZE_BITS | materialY;
                byte material = toMesh[index];
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
                        if (toMesh[faceEndX << CHUNK_SIZE_BITS | y] != material) break x_expansion;
                faceEndX--; // Account for increment then checks

                // Remove face from bitmap
                mask = ~mask;
                for (int x = materialX; x <= faceEndX; x++) toMeshFacesMap[x] &= mask;

                // Add face
                if (Material.isSemiTransparentMaterial(material))
                    addFace(glassVerticesList, side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndX - materialX);
                else if (material == WATER)
                    addFace(waterVerticesList, side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndX - materialX);
                else
                    addFace(opaqueVerticesLists[side], side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndX - materialX);
            }
    }


    private void addTopBottomFaces() {
        // Copy materials
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                lower[materialX << CHUNK_SIZE_BITS | materialZ] = materials[materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS];

        for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
            copyMaterialTopBottom(materialY);
            addTopBottomLayer(TOP, materialY, lower, upper);
            addTopBottomLayer(BOTTOM, materialY, upper, lower);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialTopBottom(int materialY) {
        if (materialY == CHUNK_SIZE - 1) for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                upper[materialX << CHUNK_SIZE_BITS | materialZ] = chunk.getMaterial(materialX, CHUNK_SIZE, materialZ);
        else for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                upper[materialX << CHUNK_SIZE_BITS | materialZ] = materials[materialX << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS | materialY + 1];
    }

    private void addTopBottomLayer(int side, int materialY, byte[] toMesh, byte[] occluding) {
        fillToMeshFacesMap(toMesh, occluding);

        // Generate faces
        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++) {
                if ((toMeshFacesMap[materialX] & 1L << materialZ) == 0) continue;

                int index = materialX << CHUNK_SIZE_BITS | materialZ;
                byte material = toMesh[index];
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
                if (Material.isSemiTransparentMaterial(material))
                    addFace(glassVerticesList, side, materialX, materialY, materialZ, material, faceEndX - materialX, faceEndZ - materialZ);
                else if (material == WATER)
                    addFace(waterVerticesList, side, materialX, materialY, materialZ, material, faceEndX - materialX, faceEndZ - materialZ);
                else
                    addFace(opaqueVerticesLists[side], side, materialX, materialY, materialZ, material, faceEndX - materialX, faceEndZ - materialZ);
            }
    }


    private void addWestEastFaces() {
        // Copy materials
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            System.arraycopy(materials, materialZ << CHUNK_SIZE_BITS, lower, materialZ << CHUNK_SIZE_BITS, CHUNK_SIZE);

        for (int materialX = 0; materialX < CHUNK_SIZE; materialX++) {
            copyMaterialWestEast(materialX);
            addWestEastLayer(WEST, materialX, lower, upper);
            addWestEastLayer(EAST, materialX, upper, lower);

            byte[] temp = lower;
            lower = upper;
            upper = temp;
        }
    }

    private void copyMaterialWestEast(int materialX) {
        if (materialX == CHUNK_SIZE - 1) for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++)
                upper[materialZ << CHUNK_SIZE_BITS | materialY] = chunk.getMaterial(CHUNK_SIZE, materialY, materialZ);
        else for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            System.arraycopy(materials, materialX + 1 << CHUNK_SIZE_BITS * 2 | materialZ << CHUNK_SIZE_BITS, upper, materialZ << CHUNK_SIZE_BITS, CHUNK_SIZE);
    }

    private void addWestEastLayer(int side, int materialX, byte[] toMesh, byte[] occluding) {
        fillToMeshFacesMap(toMesh, occluding);

        // Generate faces
        for (int materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
            for (int materialY = 0; materialY < CHUNK_SIZE; materialY++) {
                if ((toMeshFacesMap[materialZ] & 1L << materialY) == 0) continue;

                int index = materialZ << CHUNK_SIZE_BITS | materialY;
                byte material = toMesh[index];
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
                if (Material.isSemiTransparentMaterial(material))
                    addFace(glassVerticesList, side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndZ - materialZ);
                else if (material == WATER)
                    addFace(waterVerticesList, side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndZ - materialZ);
                else
                    addFace(opaqueVerticesLists[side], side, materialX, materialY, materialZ, material, faceEndY - materialY, faceEndZ - materialZ);
            }
    }


    private void fillToMeshFacesMap(byte[] toMesh, byte[] occluding) {
        for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE; index++) {
            byte toTestMaterial = toMesh[index];
            if (toTestMaterial == AIR) continue;
            byte occludingMaterial = occluding[index];
            if (occludes(toTestMaterial, occludingMaterial)) continue;

            toMeshFacesMap[index >> 6] |= 1L << index;
        }
    }

    private static void addFace(ArrayList<Integer> vertices, int side, int materialX, int materialY, int materialZ, byte material, int faceSize1, int faceSize2) {
        vertices.add(faceSize1 << 24 | faceSize2 << 18 | materialX << 12 | materialY << 6 | materialZ);
        vertices.add(side << 8 | Material.getTextureIndex(material) & 0xFF);
    }

    private static boolean occludes(byte toTestMaterial, byte occludingMaterial) {
        if (occludingMaterial == AIR) return false;
        if ((Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0) return true;

        if ((Material.getMaterialProperties(toTestMaterial) & OCCLUDES_SELF_ONLY) != 0)
            return toTestMaterial == occludingMaterial;
        return false;
    }

    private Chunk chunk;
    private final long[] toMeshFacesMap = new long[CHUNK_SIZE];
    private byte[] upper = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private byte[] lower = new byte[CHUNK_SIZE * CHUNK_SIZE];
    private final byte[] materials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

    private final ArrayList<Integer> waterVerticesList = new ArrayList<>(), glassVerticesList = new ArrayList<>();
    private final ArrayList<Integer>[] opaqueVerticesLists = new ArrayList[]{new ArrayList<Integer>(), new ArrayList<Integer>(), new ArrayList<Integer>(), new ArrayList<Integer>(), new ArrayList<Integer>(), new ArrayList<Integer>()};
}
