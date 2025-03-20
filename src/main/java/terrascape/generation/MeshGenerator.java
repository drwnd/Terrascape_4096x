package terrascape.generation;

import terrascape.entity.OpaqueModel;
import terrascape.server.Material;
import terrascape.dataStorage.Chunk;

import java.util.ArrayList;

import static terrascape.utils.Constants.*;

public final class MeshGenerator {

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public void generateMesh() {
        chunk.setMeshed(true);

        chunk.generateSurroundingChunks();
        if (chunk.getMaterialLength() != 1) chunk.optimizeMaterialStorage();
        chunk.generateOcclusionCullingData();

        if (chunk.getMaterialLength() == 1 && chunk.getSaveMaterial(0) == AIR) return;

        @SuppressWarnings("unchecked") ArrayList<Integer>[] vertexLists = new ArrayList[OpaqueModel.FACE_TYPE_COUNT];
        for (int index = 0; index < vertexLists.length; index++) vertexLists[index] = new ArrayList<>();


        ArrayList<Integer> waterVerticesList = new ArrayList<>();

        for (blockX = 0; blockX < CHUNK_SIZE; blockX++)
            for (blockZ = 0; blockZ < CHUNK_SIZE; blockZ++)
                for (blockY = 0; blockY < CHUNK_SIZE; blockY++) {

                    material = chunk.getSaveMaterial(blockX, blockY, blockZ);
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
            byte occludingMaterial = chunk.getMaterial(blockX + normal[0], blockY + normal[1], blockZ + normal[2]);
            if (occludesOpaque(occludingMaterial))
                continue;

            int texture = Material.getTextureIndex(material, side);

            int u = texture & 15;
            int v = texture >> 4 & 15;

            if (Material.isLeaveType(material))
                addFoliageSideToList(vertexLists[OpaqueModel.FOLIAGE_FACES_OFFSET + side], u, v);
            else if ((properties & HAS_ASKEW_FACES) != 0)
                addSideToList(u, v, vertexLists[OpaqueModel.ASKEW_FACES_INDEX]);
            else addSideToList(u, v, vertexLists[side]);
        }
    }

    private void addWaterMaterial(ArrayList<Integer> waterVerticesList) {
        for (side = 0; side < 6; side++) {
            if (!Material.isWaterMaterial(material) && (properties & NO_COLLISION) == 0)
                continue;

            byte[] normal = Material.NORMALS[side];
            byte occludingMaterial = chunk.getMaterial(blockX + normal[0], blockY + normal[1], blockZ + normal[2]);

            if (occludesWater(occludingMaterial))
                continue;

            addWaterSideToList(waterVerticesList);
        }
    }


    private void addSideToList(int u, int v, ArrayList<Integer> list) {
        this.list = list;

        switch (side) {
            case NORTH:
                addVertexToList(blockX + 1, blockY + 1, blockZ + 1, u + 1, v);
                addVertexToList(blockX, blockY + 1, blockZ + 1, u, v);
                addVertexToList(blockX + 1, blockY, blockZ + 1, u + 1, v + 1);
                addVertexToList(blockX, blockY, blockZ + 1, u, v + 1);
                break;
            case TOP:
                addVertexToList(blockX, blockY + 1, blockZ, u + 1, v);
                addVertexToList(blockX, blockY + 1, blockZ + 1, u, v);
                addVertexToList(blockX + 1, blockY + 1, blockZ, u + 1, v + 1);
                addVertexToList(blockX + 1, blockY + 1, blockZ + 1, u, v + 1);
                break;
            case WEST:
                addVertexToList(blockX + 1, blockY + 1, blockZ, u + 1, v);
                addVertexToList(blockX + 1, blockY + 1, blockZ + 1, u, v);
                addVertexToList(blockX + 1, blockY, blockZ, u + 1, v + 1);
                addVertexToList(blockX + 1, blockY, blockZ + 1, u, v + 1);
                break;
            case SOUTH:
                addVertexToList(blockX, blockY + 1, blockZ, u + 1, v);
                addVertexToList(blockX + 1, blockY + 1, blockZ, u, v);
                addVertexToList(blockX, blockY, blockZ, u + 1, v + 1);
                addVertexToList(blockX + 1, blockY, blockZ, u, v + 1);
                break;
            case BOTTOM:
                addVertexToList(blockX + 1, blockY, blockZ + 1, u + 1, v);
                addVertexToList(blockX, blockY, blockZ + 1, u, v);
                addVertexToList(blockX + 1, blockY, blockZ, u + 1, v + 1);
                addVertexToList(blockX, blockY, blockZ, u, v + 1);
                break;
            case EAST:
                addVertexToList(blockX, blockY + 1, blockZ + 1, u + 1, v);
                addVertexToList(blockX, blockY + 1, blockZ, u, v);
                addVertexToList(blockX, blockY, blockZ + 1, u + 1, v + 1);
                addVertexToList(blockX, blockY, blockZ, u, v + 1);
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
                addWaterVertexToList(blockX + 1, blockY + 1, blockZ + 1, u + 1, v);
                addWaterVertexToList(blockX, blockY + 1, blockZ + 1, u, v);
                addWaterVertexToList(blockX + 1, blockY, blockZ + 1, u + 1, v + 1);
                addWaterVertexToList(blockX, blockY, blockZ + 1, u, v + 1);
                break;
            case TOP:
                addWaterVertexToList(blockX, blockY + 1, blockZ, u + 1, v);
                addWaterVertexToList(blockX, blockY + 1, blockZ + 1, u, v);
                addWaterVertexToList(blockX + 1, blockY + 1, blockZ, u + 1, v + 1);
                addWaterVertexToList(blockX + 1, blockY + 1, blockZ + 1, u, v + 1);
                break;
            case WEST:
                addWaterVertexToList(blockX + 1, blockY + 1, blockZ, u + 1, v);
                addWaterVertexToList(blockX + 1, blockY + 1, blockZ + 1, u, v);
                addWaterVertexToList(blockX + 1, blockY, blockZ, u + 1, v + 1);
                addWaterVertexToList(blockX + 1, blockY, blockZ + 1, u, v + 1);
                break;
            case SOUTH:
                addWaterVertexToList(blockX, blockY + 1, blockZ, u + 1, v);
                addWaterVertexToList(blockX + 1, blockY + 1, blockZ, u, v);
                addWaterVertexToList(blockX, blockY, blockZ, u + 1, v + 1);
                addWaterVertexToList(blockX + 1, blockY, blockZ, u, v + 1);
                break;
            case BOTTOM:
                addWaterVertexToList(blockX + 1, blockY, blockZ + 1, u + 1, v);
                addWaterVertexToList(blockX, blockY, blockZ + 1, u, v);
                addWaterVertexToList(blockX + 1, blockY, blockZ, u + 1, v + 1);
                addWaterVertexToList(blockX, blockY, blockZ, u, v + 1);
                break;
            case EAST:
                addWaterVertexToList(blockX, blockY + 1, blockZ + 1, u + 1, v);
                addWaterVertexToList(blockX, blockY + 1, blockZ, u, v);
                addWaterVertexToList(blockX, blockY, blockZ + 1, u + 1, v + 1);
                addWaterVertexToList(blockX, blockY, blockZ, u, v + 1);
                break;
        }
    }

    private void addWaterVertexToList(int inChunkX, int inChunkY, int inChunkZ, int u, int v) {
        list.add(packData1((inChunkX << 4) + 15, (inChunkY << 4) + 15, (inChunkZ << 4) + 15));
        list.add(packWaterData((u << 4) + 15, (v << 4) + 15));
    }


    private void addFoliageSideToList(ArrayList<Integer> list, int u, int v) {
        this.list = list;
        int adder1, adder2;

        switch (side) {
            case NORTH:
                adder1 = (properties & ROTATE_NORTH_TEXTURE) != 0 ? 0 : 1;
                adder2 = 1 - adder1;
                addFoliageVertexToList(blockX + 1, blockY + 1, blockZ + 1, u + adder1, v + adder2);
                addFoliageVertexToList(blockX, blockY + 1, blockZ + 1, u, v);
                addFoliageVertexToList(blockX + 1, blockY, blockZ + 1, u + 1, v + 1);
                addFoliageVertexToList(blockX, blockY, blockZ + 1, u + adder2, v + adder1);
                break;
            case TOP:
                adder1 = (properties & ROTATE_TOP_TEXTURE) != 0 ? 0 : 1;
                adder2 = 1 - adder1;
                addFoliageVertexToList(blockX, blockY + 1, blockZ, u + adder1, v + adder2);
                addFoliageVertexToList(blockX, blockY + 1, blockZ + 1, u, v);
                addFoliageVertexToList(blockX + 1, blockY + 1, blockZ, u + 1, v + 1);
                addFoliageVertexToList(blockX + 1, blockY + 1, blockZ + 1, u + adder2, v + adder1);
                break;
            case WEST:
                adder1 = (properties & ROTATE_WEST_TEXTURE) != 0 ? 0 : 1;
                adder2 = 1 - adder1;
                addFoliageVertexToList(blockX + 1, blockY + 1, blockZ, u + adder1, v + adder2);
                addFoliageVertexToList(blockX + 1, blockY + 1, blockZ + 1, u, v);
                addFoliageVertexToList(blockX + 1, blockY, blockZ, u + 1, v + 1);
                addFoliageVertexToList(blockX + 1, blockY, blockZ + 1, u + adder2, v + adder1);
                break;
            case SOUTH:
                adder1 = (properties & ROTATE_SOUTH_TEXTURE) != 0 ? 0 : 1;
                adder2 = 1 - adder1;
                addFoliageVertexToList(blockX, blockY + 1, blockZ, u + adder1, v + adder2);
                addFoliageVertexToList(blockX + 1, blockY + 1, blockZ, u, v);
                addFoliageVertexToList(blockX, blockY, blockZ, u + 1, v + 1);
                addFoliageVertexToList(blockX + 1, blockY, blockZ, u + adder2, v + adder1);
                break;
            case BOTTOM:
                adder1 = (properties & ROTATE_BOTTOM_TEXTURE) != 0 ? 0 : 1;
                adder2 = 1 - adder1;
                addFoliageVertexToList(blockX + 1, blockY, blockZ + 1, u + adder1, v + adder2);
                addFoliageVertexToList(blockX, blockY, blockZ + 1, u, v);
                addFoliageVertexToList(blockX + 1, blockY, blockZ, u + 1, v + 1);
                addFoliageVertexToList(blockX, blockY, blockZ, u + adder2, v + adder1);
                break;
            case EAST:
                adder1 = (properties & ROTATE_EAST_TEXTURE) != 0 ? 0 : 1;
                adder2 = 1 - adder1;
                addFoliageVertexToList(blockX, blockY + 1, blockZ + 1, u + adder1, v + adder2);
                addFoliageVertexToList(blockX, blockY + 1, blockZ, u, v);
                addFoliageVertexToList(blockX, blockY, blockZ + 1, u + 1, v + 1);
                addFoliageVertexToList(blockX, blockY, blockZ, u + adder2, v + adder1);
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


    private static boolean occludesOpaque(byte occludingMaterial) {
        return (Material.getMaterialProperties(occludingMaterial) & NO_COLLISION) == 0;
    }

    private static boolean occludesWater(byte occludingMaterial) {
        return occludingMaterial == WATER || (Material.getMaterialProperties(occludingMaterial) & NO_COLLISION) == 0;
    }

    private Chunk chunk;

    private int blockX, blockY, blockZ;
    private int side, properties;
    private byte material;
    private ArrayList<Integer> list;
}
