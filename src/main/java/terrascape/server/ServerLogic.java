package terrascape.server;

import org.lwjgl.opengl.GL46;
import terrascape.dataStorage.octree.Chunk;
import terrascape.dataStorage.FileManager;
import terrascape.entity.*;
import terrascape.generation.ChunkGenerator;
import terrascape.player.*;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedList;


import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

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
        int chunkX = x >> CHUNK_SIZE_BITS;
        int chunkY = y >> CHUNK_SIZE_BITS;
        int chunkZ = z >> CHUNK_SIZE_BITS;

        int inChunkX = x & CHUNK_SIZE_MASK;
        int inChunkY = y & CHUNK_SIZE_MASK;
        int inChunkZ = z & CHUNK_SIZE_MASK;

        Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ, 0);
        if (chunk == null) return;

        byte previousMaterial = chunk.getSaveMaterial(inChunkX, inChunkY, inChunkZ);
        if (previousMaterial == material) return;

        int mask = -(1 << size);
        inChunkX &= mask;
        inChunkY &= mask;
        inChunkZ &= mask;

        chunk.placeMaterial(inChunkX, inChunkY, inChunkZ, material, size);

        int minX = chunkX, maxX = chunkX;
        int minY = chunkY, maxY = chunkY;
        int minZ = chunkZ, maxZ = chunkZ;

        if (inChunkX == 0) minX = chunkX - 1;
        else if (inChunkX == CHUNK_SIZE - 1 >> size) maxX = chunkX + 1;
        if (inChunkY == 0) minY = chunkY - 1;
        else if (inChunkY == CHUNK_SIZE - 1 >> size) maxY = chunkY + 1;
        if (inChunkZ == 0) minZ = chunkZ - 1;
        else if (inChunkZ == CHUNK_SIZE - 1 >> size) maxZ = chunkZ + 1;

        for (chunkX = minX; chunkX <= maxX; chunkX++)
            for (chunkY = minY; chunkY <= maxY; chunkY++)
                for (chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                    Chunk toMeshChunk = Chunk.getChunk(chunkX, chunkY, chunkZ, 0); // TODO
                    if (toMeshChunk == null) continue;
                    toMeshChunk.setMeshed(false);
                }
        restartGenerator(NONE);

        boolean previousMaterialWaterLogged = Material.isWaterMaterial(previousMaterial);
        boolean newMaterialWaterLogged = Material.isWaterMaterial(material);

        SoundManager sound = Launcher.getSound();

        if (previousMaterialWaterLogged || !newMaterialWaterLogged) {
            sound.playRandomSound(Material.getDigSound(previousMaterial), x + 0.5f, y + 0.5f, z + 0.5f, 0.0f, 0.0f, 0.0f, DIG_GAIN);
            sound.playRandomSound(Material.getFootstepsSound(material), x + 0.5f, y + 0.5f, z + 0.5f, 0.0f, 0.0f, 0.0f, STEP_GAIN);
        } else
            sound.playRandomSound(Material.getFootstepsSound(WATER), x + 0.5f, y + 0.5f, z + 0.5f, 0.0f, 0.0f, 0.0f, STEP_GAIN);
    }

    public static void bufferChunkMesh(Chunk chunk) {
        int chunkIndex = chunk.getIndex();
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

    public static void updateGT(long tick) {
        player.updateGT(tick);

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
                if (chunk.isModified() && chunk.LOD == 0) FileManager.saveChunk(chunk);
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

                if (Math.abs(chunk.X - lodPlayerX) <= RENDER_DISTANCE_XZ + 2 &&
                        Math.abs(chunk.Z - lodPlayerZ) <= RENDER_DISTANCE_XZ + 2 &&
                        Math.abs(chunk.Y - lodPlayerY) <= RENDER_DISTANCE_Y + 2)
                    continue;

                chunk.clearMesh();
                addToUnloadChunk(chunk);

                Chunk.setNull(chunk.getIndex(), chunk.LOD);
            }

        synchronized (TO_BUFFER_CHUNKS) {
            for (Iterator<Chunk> iterator = TO_BUFFER_CHUNKS.iterator(); iterator.hasNext(); ) {
                Chunk chunk = iterator.next();

                int lodPlayerX = playerChunkX >> chunk.LOD;
                int lodPlayerY = playerChunkY >> chunk.LOD;
                int lodPlayerZ = playerChunkZ >> chunk.LOD;

                if (Math.abs(chunk.X - lodPlayerX) <= RENDER_DISTANCE_XZ + 2 &&
                        Math.abs(chunk.Z - lodPlayerZ) <= RENDER_DISTANCE_XZ + 2 &&
                        Math.abs(chunk.Y - lodPlayerY) <= RENDER_DISTANCE_Y + 2)
                    continue;

                iterator.remove();
            }
        }
    }

    public static void deleteChunkMeshBuffers(Chunk chunk) {
        int chunkIndex = chunk.getIndex();
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

    private static final LinkedList<Chunk> TO_BUFFER_CHUNKS = new LinkedList<>();
    private static final LinkedList<Chunk> TO_UNLOAD_CHUNKS = new LinkedList<>();
    private static ChunkGenerator generator;

    private static Player player;

    private static byte generatorRestartScheduled = 0;

    private ServerLogic() {
    }
}
