package terrascape.server;

import org.lwjgl.opengl.GL46;
import terrascape.dataStorage.octree.Chunk;
import terrascape.dataStorage.FileManager;
import terrascape.entity.*;
import terrascape.generation.ChunkGenerator;
import terrascape.generation.WorldGeneration;
import terrascape.player.*;
import org.lwjgl.opengl.GL11;
import terrascape.utils.Utils;

import java.util.Iterator;
import java.util.LinkedList;

import static terrascape.utils.Constants.*;

public final class ServerLogic {

    public static void init() throws Exception {

        player = FileManager.loadPlayer();

        ChunkGenerator generator = new ChunkGenerator();
        generator.generateSurrounding(player);

        startGenerator();
    }

    public static void restartGenerator(int direction) {
        generatorRestartScheduled = (byte) (0x80 | direction);
    }

    public static void startGenerator() {
        generator = new ChunkGenerator();
        generator.restart(NONE);
    }

    public static void placeMaterial(byte material, int x, int y, int z, int size) {
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            int mask = -(1 << size);
            if (Integer.numberOfTrailingZeros(x & mask) < lod
                    || Integer.numberOfTrailingZeros(y & mask) < lod
                    || Integer.numberOfTrailingZeros(z & mask) < lod) break;

            int chunkX = x >> CHUNK_SIZE_BITS + lod;
            int chunkY = y >> CHUNK_SIZE_BITS + lod;
            int chunkZ = z >> CHUNK_SIZE_BITS + lod;

            int inChunkX = x >> lod & CHUNK_SIZE_MASK;
            int inChunkY = y >> lod & CHUNK_SIZE_MASK;
            int inChunkZ = z >> lod & CHUNK_SIZE_MASK;

            int lodSize = Math.max(0, size - lod);
            mask = -(1 << lodSize);
            inChunkX &= mask;
            inChunkY &= mask;
            inChunkZ &= mask;

            long expectedId = Utils.getChunkId(chunkX, chunkY, chunkZ);
            Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ, lod);
            if (chunk == null || chunk.ID != expectedId) {
                if (chunk != null) addToUnloadChunk(chunk);
                chunk = FileManager.getChunk(expectedId, lod);
            }
            if (chunk == null) {
                chunk = new Chunk(chunkX, chunkY, chunkZ, lod);
                WorldGeneration.generate(chunk);
            }
            Chunk.storeChunk(chunk);

            chunk.placeMaterial(inChunkX, inChunkY, inChunkZ, material, lodSize);

            unMeshChunkIfPresent(chunkX, chunkY, chunkZ, lod);
            if (inChunkX == 0) unMeshChunkIfPresent(chunkX - 1, chunkY, chunkZ, lod);
            if (inChunkY == 0) unMeshChunkIfPresent(chunkX, chunkY - 1, chunkZ, lod);
            if (inChunkZ == 0) unMeshChunkIfPresent(chunkX, chunkY, chunkZ - 1, lod);
        }

        restartGenerator(NONE);
    }

    public static void bufferChunkMesh(Chunk chunk) {
        int chunkIndex = chunk.INDEX;
        OpaqueModel oldOpaqueModel = Chunk.getOpaqueModel(chunkIndex, chunk.LOD);
        if (chunk.getOpaqueVertices() != null) {
            OpaqueModel newModel = ObjectLoader.loadOpaqueModel(chunk.getOpaqueVertices(), chunk.getWorldCoordinate(), chunk.getVertexCounts(), chunk.LOD);
            Chunk.setOpaqueModel(newModel, chunkIndex, chunk.LOD);
        } else Chunk.setOpaqueModel(null, chunkIndex, chunk.LOD);

        if (oldOpaqueModel != null) GL46.glDeleteBuffers(oldOpaqueModel.verticesBuffer);

        WaterModel oldWaterModel = Chunk.getWaterModel(chunkIndex, chunk.LOD);
        if (chunk.getWaterVertices() != null) {
            WaterModel newWaterModel = ObjectLoader.loadWaterModel(chunk.getWaterVertices(), chunk.getWorldCoordinate(), chunk.LOD);
            Chunk.setWaterModel(newWaterModel, chunkIndex, chunk.LOD);
        } else Chunk.setWaterModel(null, chunkIndex, chunk.LOD);

        if (oldWaterModel != null) GL46.glDeleteBuffers(oldWaterModel.verticesBuffer);

        chunk.clearMesh();
    }

    public static void update(float passedTicks) {
        loadUnloadObjects();
        player.update(passedTicks);
    }

    public static void updateGT() {
        player.updateGT();
        Particle.update();

        if (generatorRestartScheduled != 0) {
            generator.restart(generatorRestartScheduled & 0xF);
            generatorRestartScheduled = 0;
        }
    }

    public static void loadUnloadObjects() {
        synchronized (TO_UNLOAD_CHUNKS) {
            while (!TO_UNLOAD_CHUNKS.isEmpty()) {
                Chunk chunk = TO_UNLOAD_CHUNKS.removeFirst();
                deleteChunkMeshBuffers(chunk);
                if (chunk.isModified()) FileManager.saveChunk(chunk);
            }
        }

        synchronized (TO_BUFFER_CHUNKS) {
            for (int i = 0; i < MAX_CHUNKS_TO_BUFFER_PER_FRAME && !TO_BUFFER_CHUNKS.isEmpty(); i++) {
                Chunk chunk = TO_BUFFER_CHUNKS.removeFirst();
                bufferChunkMesh(chunk);
            }
        }
    }

    public static void unloadChunks(int playerChunkX, int playerChunkY, int playerChunkZ) {
        for (int lod = 0; lod < LOD_COUNT; lod++)
            for (Chunk chunk : Chunk.getWorld(lod)) {
                if (chunk == null) continue;

                int lodPlayerX = playerChunkX >> lod;
                int lodPlayerY = playerChunkY >> lod;
                int lodPlayerZ = playerChunkZ >> lod;

                if (Math.abs(chunk.X - lodPlayerX) <= RENDER_DISTANCE_XZ + 2 && Math.abs(chunk.Z - lodPlayerZ) <= RENDER_DISTANCE_XZ + 2 && Math.abs(chunk.Y - lodPlayerY) <= RENDER_DISTANCE_Y + 2)
                    continue;

                chunk.clearMesh();
                addToUnloadChunk(chunk);

                Chunk.setNull(chunk.INDEX, chunk.LOD);
            }

        synchronized (TO_BUFFER_CHUNKS) {
            for (Iterator<Chunk> iterator = TO_BUFFER_CHUNKS.iterator(); iterator.hasNext(); ) {
                Chunk chunk = iterator.next();

                int lodPlayerX = playerChunkX >> chunk.LOD;
                int lodPlayerY = playerChunkY >> chunk.LOD;
                int lodPlayerZ = playerChunkZ >> chunk.LOD;

                if (Math.abs(chunk.X - lodPlayerX) <= RENDER_DISTANCE_XZ + 2 && Math.abs(chunk.Z - lodPlayerZ) <= RENDER_DISTANCE_XZ + 2 && Math.abs(chunk.Y - lodPlayerY) <= RENDER_DISTANCE_Y + 2)
                    continue;

                iterator.remove();
            }
        }
    }

    public static void deleteChunkMeshBuffers(Chunk chunk) {
        int chunkIndex = chunk.INDEX;
        OpaqueModel opaqueModel = Chunk.getOpaqueModel(chunkIndex, chunk.LOD);
        if (opaqueModel != null) {
            GL46.glDeleteBuffers(opaqueModel.verticesBuffer);
            Chunk.setOpaqueModel(null, chunkIndex, chunk.LOD);
        }

        WaterModel waterModel = Chunk.getWaterModel(chunkIndex, chunk.LOD);
        if (waterModel != null) {
            GL46.glDeleteBuffers(waterModel.verticesBuffer);
            Chunk.setWaterModel(null, chunkIndex, chunk.LOD);
        }
    }

    public static void input() {
        player.input();
    }

    public static void render(float timeSinceLastTick) {
        WindowManager window = Launcher.getWindow();

        if (window.isResize()) {
            GL11.glViewport(0, 0, window.getWidth(), window.getHeight());
            window.setResize(true);
        }
        player.render(timeSinceLastTick);
    }

    public static void addToBufferChunk(Chunk chunk) {
        if (chunk == null) return;
        synchronized (TO_BUFFER_CHUNKS) {
            if (!TO_BUFFER_CHUNKS.contains(chunk)) TO_BUFFER_CHUNKS.add(chunk);
        }
    }

    public static void addToUnloadChunk(Chunk chunk) {
        if (chunk == null) return;
        synchronized (TO_UNLOAD_CHUNKS) {
            TO_UNLOAD_CHUNKS.add(chunk);
        }
    }

    public static Player getPlayer() {
        return player;
    }

    public static void cleanUp() {
        player.cleanUp();
        ObjectLoader.cleanUp();
        generator.cleanUp();
        FileManager.savePlayer();
        FileManager.saveAllModifiedChunks();
        FileManager.saveGameState();
    }

    public static int getAmountOfToBufferChunks() {
        return TO_BUFFER_CHUNKS.size();
    }

    private static void unMeshChunkIfPresent(int chunkX, int chunkY, int chunkZ, int lod) {
        Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ, lod);
        if (chunk == null) return;
        chunk.setMeshed(false);
    }

    private static final LinkedList<Chunk> TO_BUFFER_CHUNKS = new LinkedList<>();
    private static final LinkedList<Chunk> TO_UNLOAD_CHUNKS = new LinkedList<>();
    private static ChunkGenerator generator;

    private static Player player;

    private static byte generatorRestartScheduled = 0;

    private ServerLogic() {
    }
}
