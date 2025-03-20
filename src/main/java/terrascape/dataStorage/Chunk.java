package terrascape.dataStorage;

import org.joml.Math;
import terrascape.entity.WaterModel;
import terrascape.entity.OpaqueModel;
import terrascape.generation.WorldGeneration;
import terrascape.server.ServerLogic;
import org.joml.Vector3i;
import terrascape.utils.Utils;

import java.util.Arrays;

import static terrascape.utils.Constants.*;

public final class Chunk {

    public final int X, Y, Z;
    public final long ID;

    public Chunk(int x, int y, int z) {
        this.X = x;
        this.Y = y;
        this.Z = z;
        worldCoordinate = new Vector3i(X << CHUNK_SIZE_BITS, Y << CHUNK_SIZE_BITS, Z << CHUNK_SIZE_BITS);

        materials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

        ID = Utils.getChunkId(X, Y, Z);
        index = Utils.getChunkIndex(X, Y, Z);
    }

    public Chunk(int x, int y, int z, byte[] materials) {
        this.X = x;
        this.Y = y;
        this.Z = z;
        worldCoordinate = new Vector3i(X << CHUNK_SIZE_BITS, Y << CHUNK_SIZE_BITS, Z << CHUNK_SIZE_BITS);

        this.materials = materials;

        ID = Utils.getChunkId(X, Y, Z);
        index = Utils.getChunkIndex(X, Y, Z);
    }

    public void optimizeMaterialStorage() {
        byte firstMaterial = materials[0];
        for (byte material : materials)
            if (material != firstMaterial) return;

        materials = new byte[]{firstMaterial};
    }

    public static boolean readOcclusionCullingSidePair(int entrySide, int exitSide, short occlusionCullingData) {
        if (entrySide == exitSide) return false;
        int largerSide = Math.max(entrySide, exitSide);
        int smallerSide = Math.min(entrySide, exitSide);

        return (occlusionCullingData & 1 << (largerSide * (largerSide - 1) >> 1) + smallerSide) != 0;
    }

    public void generateOcclusionCullingData() {
        Chunk.occlusionCullingData[index] = (short) 0x7FFF;
    }

    public void generateSurroundingChunks() {
        for (int chunkX = X - 1; chunkX <= X + 1; chunkX++)
            for (int chunkZ = Z - 1; chunkZ <= Z + 1; chunkZ++)
                for (int chunkY = Y - 1; chunkY <= Y + 1; chunkY++) {

                    long expectedId = Utils.getChunkId(chunkX, chunkY, chunkZ);
                    int index = Utils.getChunkIndex(chunkX, chunkY, chunkZ);
                    Chunk chunk = getChunk(index);

                    if (chunk == null) {
                        System.err.println("surrounding Chunk is null");
                        chunk = FileManager.getChunk(expectedId);
                        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ);

                        storeChunk(chunk);
                        if (!chunk.isGenerated) WorldGeneration.generate(chunk);

                    } else if (chunk.ID != expectedId) {
                        System.err.println("surrounding Chunk is not correct");
                        ServerLogic.addToUnloadChunk(chunk);

                        chunk = FileManager.getChunk(expectedId);
                        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ);

                        Chunk.storeChunk(chunk);
                        if (!chunk.isGenerated) WorldGeneration.generate(chunk);

                    } else if (!chunk.isGenerated) {
                        System.err.println("surrounding Chunk is not generated");
                        WorldGeneration.generate(chunk);
                    }
                }
    }

    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkX < 0) {
            Chunk neighbor = getChunk(X - 1, Y, Z);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(CHUNK_SIZE + inChunkX, inChunkY, inChunkZ);
        } else if (inChunkX >= CHUNK_SIZE) {
            Chunk neighbor = getChunk(X + 1, Y, Z);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX - CHUNK_SIZE, inChunkY, inChunkZ);
        }
        if (inChunkY < 0) {
            Chunk neighbor = getChunk(X, Y - 1, Z);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX, CHUNK_SIZE + inChunkY, inChunkZ);
        } else if (inChunkY >= CHUNK_SIZE) {
            Chunk neighbor = getChunk(X, Y + 1, Z);
            if (neighbor == null) {
                return OUT_OF_WORLD;
            }
            return neighbor.getSaveMaterial(inChunkX, inChunkY - CHUNK_SIZE, inChunkZ);
        }
        if (inChunkZ < 0) {
            Chunk neighbor = getChunk(X, Y, Z - 1);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX, inChunkY, CHUNK_SIZE + inChunkZ);
        } else if (inChunkZ >= CHUNK_SIZE) {
            Chunk neighbor = getChunk(X, Y, Z + 1);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX, inChunkY, inChunkZ - CHUNK_SIZE);
        }

        return getSaveMaterial(inChunkX, inChunkY, inChunkZ);
    }

    public byte getSaveMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        int index = inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY;
        return materials[materials.length <= index ? 0 : index];
    }

    public byte getSaveMaterial(int index) {
        return materials[materials.length <= index ? 0 : index];
    }

    public static byte getMaterialInWorld(int x, int y, int z) {
        Chunk chunk = world[Utils.getChunkIndex(x >> CHUNK_SIZE_BITS, y >> CHUNK_SIZE_BITS, z >> CHUNK_SIZE_BITS)];
        if (chunk == null || !chunk.isGenerated) return OUT_OF_WORLD;
        return chunk.getSaveMaterial(x & CHUNK_SIZE_MASK, y & CHUNK_SIZE_MASK, z & CHUNK_SIZE_MASK);
    }

    public void placeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        if (materials.length == 1 && materials[0] == material) return;
        if (materials.length == 1) {
            byte oldMaterial = materials[0];
            materials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
            Arrays.fill(materials, oldMaterial);
        }
        storeSave(inChunkX, inChunkY, inChunkZ, material);
        setModified();
    }

    public void storeSave(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        materials[inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY] = material;
    }

    public static Chunk getChunk(int chunkX, int chunkY, int chunkZ) {
        return world[Utils.getChunkIndex(chunkX, chunkY, chunkZ)];
    }

    public static Chunk getChunk(int index) {
        return world[index];
    }

    public static OpaqueModel getOpaqueModel(int index) {
        return opaqueModels[index];
    }

    public static void setOpaqueModel(OpaqueModel model, int index) {
        opaqueModels[index] = model;
    }

    public static void setWaterModel(WaterModel waterModel, int index) {
        waterModels[index] = waterModel;
    }

    public static WaterModel getWaterModel(int index) {
        return waterModels[index];
    }

    public static void storeChunk(Chunk chunk) {
        world[chunk.getIndex()] = chunk;
    }

    public static void setNull(int index) {
        world[index] = null;
        occlusionCullingData[index] = (short) 0;
    }

    public int[] getOpaqueVertices() {
        return opaqueVertices;
    }

    public int[] getWaterVertices() {
        return waterVertices;
    }

    public Vector3i getWorldCoordinate() {
        return worldCoordinate;
    }

    public void clearMesh() {
        opaqueVertices = new int[0];
        vertexCounts = new int[0];
        waterVertices = new int[0];
    }

    public int getIndex() {
        return index;
    }

    public boolean isMeshed() {
        return isMeshed;
    }

    public void setMeshed(boolean meshed) {
        isMeshed = meshed;
    }

    public boolean isGenerated() {
        return isGenerated;
    }

    public void setGenerated() {
        isGenerated = true;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified() {
        isModified = true;
    }

    public static Chunk[] getWorld() {
        return world;
    }

    public static int getOcclusionCullingDamper(short occlusionCullingData) {
        return occlusionCullingData >> 15 & 1;
    }

    public static short getOcclusionCullingData(int index) {
        return occlusionCullingData[index];
    }

    public boolean isMaterialOptimized() {
        return materials.length == 1;
    }

    public int getMaterialLength() {
        return materials.length;
    }

    public byte[] getMaterials() {
        return materials;
    }

    public void setSaved() {
        // TODO
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int[] getVertexCounts() {
        return vertexCounts;
    }

    public void setVertexCounts(int[] vertexCounts) {
        this.vertexCounts = vertexCounts;
    }

    public static void setStaticData(Chunk[] world, short[] occlusionCullingData, OpaqueModel[] opaqueModels, WaterModel[] waterModels) {
        Chunk.world = world;
        Chunk.occlusionCullingData = occlusionCullingData;
        Chunk.opaqueModels = opaqueModels;
        Chunk.waterModels = waterModels;
    }

    public void setWaterVertices(int[] waterVertices) {
        this.waterVertices = waterVertices;
    }

    public void setOpaqueVertices(int[] opaqueVertices) {
        this.opaqueVertices = opaqueVertices;
    }

    public static boolean isValidPosition(int inChunkX, int inChunkY, int inChunkZ) {
        return (inChunkX & CHUNK_SIZE_MASK) == inChunkX && (inChunkY & CHUNK_SIZE_MASK) == inChunkY && (inChunkZ & CHUNK_SIZE_MASK) == inChunkZ;
    }

    public static int countOpaqueModels() {
        int counter = 0;
        for (OpaqueModel model : opaqueModels) if (model != null) counter++;
        return counter;
    }

    public static int countWaterModels() {
        int counter = 0;
        for (WaterModel model : waterModels) if (model != null) counter++;
        return counter;
    }

    private static Chunk[] world;
    private static OpaqueModel[] opaqueModels;
    private static WaterModel[] waterModels;
    private static short[] occlusionCullingData;

    private byte[] materials;

    private int[] waterVertices = new int[0];
    private int[] vertexCounts = new int[0];
    private int[] opaqueVertices = new int[0];

    private final Vector3i worldCoordinate;
    private int index;

    private boolean isMeshed = false;
    private boolean isGenerated = false;
    private boolean isModified = false;
}
