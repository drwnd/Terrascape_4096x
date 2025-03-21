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

        @SuppressWarnings("unchecked") ArrayList<Integer>[] vertexLists = new ArrayList[OpaqueModel.FACE_TYPE_COUNT];
        for (int index = 0; index < vertexLists.length; index++) vertexLists[index] = new ArrayList<>();


        ArrayList<Integer> waterVerticesList = new ArrayList<>();

        for (materialX = 0; materialX < CHUNK_SIZE; materialX++)
            for (materialZ = 0; materialZ < CHUNK_SIZE; materialZ++)
                for (materialY = 0; materialY < CHUNK_SIZE; materialY++) {

                    material = chunk.getSaveMaterial(materialX, materialY, materialZ);
                    properties = Material.getMaterialProperties(material);

                    if (material == AIR) continue;

                    if (!Material.isWaterMaterial(material)) addOpaqueMaterial(vertexLists);
                    if (Material.isWaterMaterial(material)) addWaterMaterial(waterVerticesList);
                }

        int[] waterVertices = new int[waterVerticesList.size()];
        for (int i = 0, size = waterVerticesList.size(); i < size; i++)
            waterVertices[i] = waterVerticesList.get(i);
        chunk.setWaterVertices(waterVertices);

        int totalVertexCount = 0, verticesIndex = 0;
        for (ArrayList<Integer> vertexList : vertexLists) totalVertexCount += vertexList.size();
        int[] vertexCounts = new int[vertexLists.length];
        int[] opaqueVertices = new int[totalVertexCount];

        for (int index = 0; index < vertexLists.length; index++) {
            ArrayList<Integer> vertexList = vertexLists[index];
            vertexCounts[index] = (int) (vertexList.size() * 0.75);
            for (int vertex : vertexList) opaqueVertices[verticesIndex++] = vertex;
        }

        chunk.setOpaqueVertices(opaqueVertices);
        chunk.setVertexCounts(vertexCounts);
    }

    private void addOpaqueMaterial(ArrayList<Integer>[] vertexLists) {
        for (side = 0; side < 6; side++) {
            byte[] normal = Material.NORMALS[side];
            byte occludingMaterial = chunk.getMaterial(materialX + normal[0], materialY + normal[1], materialZ + normal[2]);
            if (occludesOpaque(material, occludingMaterial))
                continue;

            int texture = Material.getTextureIndex(material);

            int u = texture & 15;
            int v = texture >> 4 & 15;

            if (Material.isLeaveType(material))
                addFoliageSideToList(vertexLists[OpaqueModel.FOLIAGE_FACES_OFFSET + side], u, v);
            else addSideToList(u, v, vertexLists[side]);
        }
    }

    private void addWaterMaterial(ArrayList<Integer> waterVerticesList) {
        for (side = 0; side < 6; side++) {
            if (!Material.isWaterMaterial(material) && (properties & NO_COLLISION) == 0)
                continue;

            byte[] normal = Material.NORMALS[side];
            byte occludingMaterial = chunk.getMaterial(materialX + normal[0], materialY + normal[1], materialZ + normal[2]);

            if (occludesWater(occludingMaterial))
                continue;

            addWaterSideToList(waterVerticesList);
        }
    }


    private void addSideToList(int u, int v, ArrayList<Integer> list) {
        this.list = list;

        switch (side) {
            case NORTH:
                addVertexToList(materialX + 1, materialY + 1, materialZ + 1, u + 1, v);
                addVertexToList(materialX, materialY + 1, materialZ + 1, u, v);
                addVertexToList(materialX + 1, materialY, materialZ + 1, u + 1, v + 1);
                addVertexToList(materialX, materialY, materialZ + 1, u, v + 1);
                break;
            case TOP:
                addVertexToList(materialX, materialY + 1, materialZ, u + 1, v);
                addVertexToList(materialX, materialY + 1, materialZ + 1, u, v);
                addVertexToList(materialX + 1, materialY + 1, materialZ, u + 1, v + 1);
                addVertexToList(materialX + 1, materialY + 1, materialZ + 1, u, v + 1);
                break;
            case WEST:
                addVertexToList(materialX + 1, materialY + 1, materialZ, u + 1, v);
                addVertexToList(materialX + 1, materialY + 1, materialZ + 1, u, v);
                addVertexToList(materialX + 1, materialY, materialZ, u + 1, v + 1);
                addVertexToList(materialX + 1, materialY, materialZ + 1, u, v + 1);
                break;
            case SOUTH:
                addVertexToList(materialX, materialY + 1, materialZ, u + 1, v);
                addVertexToList(materialX + 1, materialY + 1, materialZ, u, v);
                addVertexToList(materialX, materialY, materialZ, u + 1, v + 1);
                addVertexToList(materialX + 1, materialY, materialZ, u, v + 1);
                break;
            case BOTTOM:
                addVertexToList(materialX + 1, materialY, materialZ + 1, u + 1, v);
                addVertexToList(materialX, materialY, materialZ + 1, u, v);
                addVertexToList(materialX + 1, materialY, materialZ, u + 1, v + 1);
                addVertexToList(materialX, materialY, materialZ, u, v + 1);
                break;
            case EAST:
                addVertexToList(materialX, materialY + 1, materialZ + 1, u + 1, v);
                addVertexToList(materialX, materialY + 1, materialZ, u, v);
                addVertexToList(materialX, materialY, materialZ + 1, u + 1, v + 1);
                addVertexToList(materialX, materialY, materialZ, u, v + 1);
                break;
        }
    }

    private void addVertexToList(int inChunkX, int inChunkY, int inChunkZ, int u, int v) {
        list.add(packData1((inChunkX << 4) + 15, (inChunkY << 4) + 15, (inChunkZ << 4) + 15));
        list.add(packData2((u << 4) + 15, (v << 4) + 15));
    }


    private void addWaterSideToList(ArrayList<Integer> list) {
        this.list = list;
        int u = (byte) 64 & 15;
        int v = (byte) 64 >> 4 & 15;

        switch (side) {
            case NORTH:
                addWaterVertexToList(materialX + 1, materialY + 1, materialZ + 1, u + 1, v);
                addWaterVertexToList(materialX, materialY + 1, materialZ + 1, u, v);
                addWaterVertexToList(materialX + 1, materialY, materialZ + 1, u + 1, v + 1);
                addWaterVertexToList(materialX, materialY, materialZ + 1, u, v + 1);
                break;
            case TOP:
                addWaterVertexToList(materialX, materialY + 1, materialZ, u + 1, v);
                addWaterVertexToList(materialX, materialY + 1, materialZ + 1, u, v);
                addWaterVertexToList(materialX + 1, materialY + 1, materialZ, u + 1, v + 1);
                addWaterVertexToList(materialX + 1, materialY + 1, materialZ + 1, u, v + 1);
                break;
            case WEST:
                addWaterVertexToList(materialX + 1, materialY + 1, materialZ, u + 1, v);
                addWaterVertexToList(materialX + 1, materialY + 1, materialZ + 1, u, v);
                addWaterVertexToList(materialX + 1, materialY, materialZ, u + 1, v + 1);
                addWaterVertexToList(materialX + 1, materialY, materialZ + 1, u, v + 1);
                break;
            case SOUTH:
                addWaterVertexToList(materialX, materialY + 1, materialZ, u + 1, v);
                addWaterVertexToList(materialX + 1, materialY + 1, materialZ, u, v);
                addWaterVertexToList(materialX, materialY, materialZ, u + 1, v + 1);
                addWaterVertexToList(materialX + 1, materialY, materialZ, u, v + 1);
                break;
            case BOTTOM:
                addWaterVertexToList(materialX + 1, materialY, materialZ + 1, u + 1, v);
                addWaterVertexToList(materialX, materialY, materialZ + 1, u, v);
                addWaterVertexToList(materialX + 1, materialY, materialZ, u + 1, v + 1);
                addWaterVertexToList(materialX, materialY, materialZ, u, v + 1);
                break;
            case EAST:
                addWaterVertexToList(materialX, materialY + 1, materialZ + 1, u + 1, v);
                addWaterVertexToList(materialX, materialY + 1, materialZ, u, v);
                addWaterVertexToList(materialX, materialY, materialZ + 1, u + 1, v + 1);
                addWaterVertexToList(materialX, materialY, materialZ, u, v + 1);
                break;
        }
    }

    private void addWaterVertexToList(int inChunkX, int inChunkY, int inChunkZ, int u, int v) {
        list.add(packData1((inChunkX << 4) + 15, (inChunkY << 4) + 15, (inChunkZ << 4) + 15));
        list.add(packWaterData((u << 4) + 15, (v << 4) + 15));
    }


    private void addFoliageSideToList(ArrayList<Integer> list, int u, int v) {
        this.list = list;

        switch (side) {
            case NORTH:
                addFoliageVertexToList(materialX + 1, materialY + 1, materialZ + 1, u + 1, v);
                addFoliageVertexToList(materialX, materialY + 1, materialZ + 1, u, v);
                addFoliageVertexToList(materialX + 1, materialY, materialZ + 1, u + 1, v + 1);
                addFoliageVertexToList(materialX, materialY, materialZ + 1, u, v + 1);
                break;
            case TOP:
                addFoliageVertexToList(materialX, materialY + 1, materialZ, u + 1, v);
                addFoliageVertexToList(materialX, materialY + 1, materialZ + 1, u, v);
                addFoliageVertexToList(materialX + 1, materialY + 1, materialZ, u + 1, v + 1);
                addFoliageVertexToList(materialX + 1, materialY + 1, materialZ + 1, u, v + 1);
                break;
            case WEST:
                addFoliageVertexToList(materialX + 1, materialY + 1, materialZ, u + 1, v);
                addFoliageVertexToList(materialX + 1, materialY + 1, materialZ + 1, u, v);
                addFoliageVertexToList(materialX + 1, materialY, materialZ, u + 1, v + 1);
                addFoliageVertexToList(materialX + 1, materialY, materialZ + 1, u, v + 1);
                break;
            case SOUTH:
                addFoliageVertexToList(materialX, materialY + 1, materialZ, u + 1, v);
                addFoliageVertexToList(materialX + 1, materialY + 1, materialZ, u, v);
                addFoliageVertexToList(materialX, materialY, materialZ, u + 1, v + 1);
                addFoliageVertexToList(materialX + 1, materialY, materialZ, u, v + 1);
                break;
            case BOTTOM:
                addFoliageVertexToList(materialX + 1, materialY, materialZ + 1, u + 1, v);
                addFoliageVertexToList(materialX, materialY, materialZ + 1, u, v);
                addFoliageVertexToList(materialX + 1, materialY, materialZ, u + 1, v + 1);
                addFoliageVertexToList(materialX, materialY, materialZ, u, v + 1);
                break;
            case EAST:
                addFoliageVertexToList(materialX, materialY + 1, materialZ + 1, u + 1, v);
                addFoliageVertexToList(materialX, materialY + 1, materialZ, u, v);
                addFoliageVertexToList(materialX, materialY, materialZ + 1, u + 1, v + 1);
                addFoliageVertexToList(materialX, materialY, materialZ, u, v + 1);
                break;
        }
    }

    private void addFoliageVertexToList(int inChunkX, int inChunkY, int inChunkZ, int u, int v) {
        list.add(packData1((inChunkX << 4) + 15, (inChunkY << 4) + 15, (inChunkZ << 4) + 15));
        list.add(packFoliageData((u << 4) + 15, (v << 4) + 15));
    }


    private int packData1(int inChunkX, int inChunkY, int inChunkZ) {
        return inChunkX << 20 | inChunkY << 10 | inChunkZ;
    }

    private int packData2(int u, int v) {
        return side << 26 | MAX_SKY_LIGHT_VALUE << 22 | u << 9 | v;
    }

    private int packWaterData(int u, int v) {
        return 1 << 29 | side << 26 | MAX_SKY_LIGHT_VALUE << 22 | u << 9 | v;
    }

    private int packFoliageData(int u, int v) {
        return 1 << 29 | side << 26 | MAX_SKY_LIGHT_VALUE << 22 | u << 9 | v;
    }


    private static boolean occludesOpaque(byte toTestMaterial, byte occludingMaterial) {
        if (toTestMaterial == LAVA)
            return occludingMaterial == LAVA || (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;
        if (Material.isGlassType(toTestMaterial)) return Material.isGlassType(occludingMaterial);
        return (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;
    }

    private static boolean occludesWater(byte occludingMaterial) {
        return occludingMaterial == WATER || (Material.getMaterialProperties(occludingMaterial) & TRANSPARENT) == 0;
    }

    private Chunk chunk;

    private int materialX, materialY, materialZ;
    private int side, properties;
    private byte material;
    private ArrayList<Integer> list;
}
