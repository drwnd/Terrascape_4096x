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
            vertexCounts[index] = vertexList.size() * 3;
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

            vertexLists[side].add(side << 29 | u << 25 | v << 21 | materialX << 14 | materialY << 7 | materialZ);
            vertexLists[side].add(0);
        }
    }

    private void addWaterMaterial(ArrayList<Integer> waterVerticesList) {
        for (side = 0; side < 6; side++) {
            byte[] normal = Material.NORMALS[side];
            byte occludingMaterial = chunk.getMaterial(materialX + normal[0], materialY + normal[1], materialZ + normal[2]);
            if (occludesWater(occludingMaterial))
                continue;

            int texture = Material.getTextureIndex(material);

            int u = texture & 15;
            int v = texture >> 4 & 15;

            waterVerticesList.add(side << 29 | u << 25 | v << 21 | materialX << 14 | materialY << 7 | materialZ);
            waterVerticesList.add(0);
        }
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
    private int side;
    private byte material;
}
