package terrascape.dataStorage.octree;

import terrascape.dataStorage.FileManager;
import terrascape.entity.WaterModel;
import terrascape.entity.OpaqueModel;
import terrascape.generation.WorldGeneration;
import terrascape.server.ServerLogic;
import org.joml.Vector3i;
import terrascape.utils.Utils;

import java.util.ArrayList;

import static terrascape.utils.Constants.*;

public final class Chunk {

    public final int X, Y, Z; // Chunk coordinates not absolute coordinates
    public final long ID;
    public final int LOD;
    public final int INDEX;

    public Chunk(int x, int y, int z, int lod) {
        this.X = x;
        this.Y = y;
        this.Z = z;
        worldCoordinate = new Vector3i(X << CHUNK_SIZE_BITS + lod, Y << CHUNK_SIZE_BITS + lod, Z << CHUNK_SIZE_BITS + lod);
        materials = new HomogenousSegment(AIR, (byte) (CHUNK_SIZE_BITS - 1));

        ID = Utils.getChunkId(X, Y, Z);
        INDEX = Utils.getChunkIndex(X, Y, Z);
        LOD = lod;
    }

    public Chunk(int x, int y, int z, int lod, ChunkSegment materials) {
        this.X = x;
        this.Y = y;
        this.Z = z;
        worldCoordinate = new Vector3i(X << CHUNK_SIZE_BITS + lod, Y << CHUNK_SIZE_BITS + lod, Z << CHUNK_SIZE_BITS + lod);
        this.materials = materials;

        ID = Utils.getChunkId(X, Y, Z);
        INDEX = Utils.getChunkIndex(X, Y, Z);
        LOD = lod;
    }

    public void generateSurroundingChunks() {
        generateSurroundingChunk(X, Y, Z + 1);
        generateSurroundingChunk(X, Y, Z - 1);
        generateSurroundingChunk(X, Y + 1, Z);
        generateSurroundingChunk(X, Y - 1, Z);
        generateSurroundingChunk(X + 1, Y, Z);
        generateSurroundingChunk(X - 1, Y, Z);
    }

    private void generateSurroundingChunk(int chunkX, int chunkY, int chunkZ) {
        long expectedId = Utils.getChunkId(chunkX, chunkY, chunkZ);
        int index = Utils.getChunkIndex(chunkX, chunkY, chunkZ);
        Chunk chunk = getChunk(index, LOD);

        if (chunk == null) {
            System.err.println("surrounding Chunk is null LOD:" + LOD);
            chunk = FileManager.getChunk(expectedId, LOD);
            if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ, LOD);

            storeChunk(chunk);
            if (!chunk.isGenerated) WorldGeneration.generate(chunk);

        } else if (chunk.ID != expectedId) {
            System.err.println("surrounding Chunk is not correct found LOD:" + chunk.LOD + " expected:" + LOD);
            ServerLogic.addToUnloadChunk(chunk);

            chunk = FileManager.getChunk(expectedId, LOD);
            if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ, LOD);

            Chunk.storeChunk(chunk);
            if (!chunk.isGenerated) WorldGeneration.generate(chunk);

        } else if (!chunk.isGenerated) {
            System.err.println("surrounding Chunk is not generated");
            WorldGeneration.generate(chunk);
        }
    }

    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkX < 0) {
            Chunk neighbor = getChunk(X - 1, Y, Z, LOD);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(CHUNK_SIZE + inChunkX, inChunkY, inChunkZ);
        } else if (inChunkX >= CHUNK_SIZE) {
            Chunk neighbor = getChunk(X + 1, Y, Z, LOD);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX - CHUNK_SIZE, inChunkY, inChunkZ);
        }
        if (inChunkY < 0) {
            Chunk neighbor = getChunk(X, Y - 1, Z, LOD);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX, CHUNK_SIZE + inChunkY, inChunkZ);
        } else if (inChunkY >= CHUNK_SIZE) {
            Chunk neighbor = getChunk(X, Y + 1, Z, LOD);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX, inChunkY - CHUNK_SIZE, inChunkZ);
        }
        if (inChunkZ < 0) {
            Chunk neighbor = getChunk(X, Y, Z - 1, LOD);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX, inChunkY, CHUNK_SIZE + inChunkZ);
        } else if (inChunkZ >= CHUNK_SIZE) {
            Chunk neighbor = getChunk(X, Y, Z + 1, LOD);
            if (neighbor == null) return OUT_OF_WORLD;
            return neighbor.getSaveMaterial(inChunkX, inChunkY, inChunkZ - CHUNK_SIZE);
        }

        return getSaveMaterial(inChunkX, inChunkY, inChunkZ);
    }

    public byte getSaveMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        return materials.getMaterial(inChunkX, inChunkY, inChunkZ);
    }

    public static byte getMaterialInWorld(int x, int y, int z) {
        Chunk chunk = world[0][Utils.getChunkIndex(x >> CHUNK_SIZE_BITS, y >> CHUNK_SIZE_BITS, z >> CHUNK_SIZE_BITS)];
        if (chunk == null || !chunk.isGenerated) return OUT_OF_WORLD;
        return chunk.getSaveMaterial(x & CHUNK_SIZE_MASK, y & CHUNK_SIZE_MASK, z & CHUNK_SIZE_MASK);
    }

    public void placeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size) {
        materials = materials.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size);
        setModified();
    }

    public void store(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        materials = materials.storeMaterial(inChunkX, inChunkY, inChunkZ, material, 0);
    }

    public static Chunk getChunk(int chunkX, int chunkY, int chunkZ, int lod) {
        return world[lod][Utils.getChunkIndex(chunkX, chunkY, chunkZ)];
    }

    public static Chunk getChunk(int index, int lod) {
        return world[lod][index];
    }

    public static OpaqueModel getOpaqueModel(int index, int lod) {
        return opaqueModels[lod][index];
    }

    public static boolean isModelPresent(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        return getOpaqueModel(Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ), lod) != null;
    }

    public static void setOpaqueModel(OpaqueModel model, int index, int lod) {
        opaqueModels[lod][index] = model;
    }

    public static void setWaterModel(WaterModel waterModel, int index, int lod) {
        waterModels[lod][index] = waterModel;
    }

    public static WaterModel getWaterModel(int index, int lod) {
        return waterModels[lod][index];
    }

    public static void storeChunk(Chunk chunk) {
        world[chunk.LOD][chunk.INDEX] = chunk;
    }

    public static void setNull(int index, int lod) {
        world[lod][index] = null;
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
        return isModified && !saved;
    }

    public void setModified() {
        isModified = true;
        saved = false;
    }

    public static Chunk[] getWorld(int lod) {
        return world[lod];
    }

    public byte[] materialsToBytes() {
        ArrayList<Byte> bytes = new ArrayList<>(materials.getByteSize());
        materials.addBytes(bytes);
        byte[] arrayBytes = new byte[bytes.size()];
        for (int index = 0; index < arrayBytes.length; index++) arrayBytes[index] = bytes.get(index);
        return arrayBytes;
    }

    public void setSaved() {
        saved = true;
    }

    public int[] getVertexCounts() {
        return vertexCounts;
    }

    public void setVertexCounts(int[] vertexCounts) {
        this.vertexCounts = vertexCounts;
    }

    public void setWaterVertices(int[] waterVertices) {
        this.waterVertices = waterVertices;
    }

    public void setOpaqueVertices(int[] opaqueVertices) {
        this.opaqueVertices = opaqueVertices;
    }

    public void setMaterials(ChunkSegment materials) {
        this.materials = materials;
    }

    public static int countOpaqueModels() {
        int counter = 0;
        for (OpaqueModel[] models : opaqueModels) for (OpaqueModel model : models) if (model != null) counter++;
        return counter;
    }

    public static int countWaterModels() {
        int counter = 0;
        for (WaterModel[] models : waterModels) for (WaterModel model : models) if (model != null) counter++;
        return counter;
    }

    public static long getByteSize(int lod) {
        Chunk[] chunks = world[lod];
        long byteSize = 0;

        for (Chunk chunk : chunks) if (chunk != null) byteSize += chunk.materials.getByteSize();

        return byteSize;
    }

    private final static Chunk[][] world = new Chunk[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH];
    private final static OpaqueModel[][] opaqueModels = new OpaqueModel[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH];
    private final static WaterModel[][] waterModels = new WaterModel[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH];

    private ChunkSegment materials;

    private int[] waterVertices = new int[0];
    private int[] vertexCounts = new int[0];
    private int[] opaqueVertices = new int[0];

    private final Vector3i worldCoordinate;

    private boolean isMeshed = false;
    private boolean isGenerated = false;
    private boolean isModified = false;
    private boolean saved = true;
}
