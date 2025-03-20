package terrascape.generation;

import terrascape.player.Player;
import terrascape.dataStorage.Chunk;
import terrascape.dataStorage.FileManager;
import terrascape.utils.Utils;
import terrascape.server.ServerLogic;
import org.joml.Vector3f;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

public final class ChunkGenerator {

    public ChunkGenerator() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
    }

    public void generateSurrounding(Player player) {
        Vector3f playerPosition = player.getCamera().getPosition();
        int playerChunkX = Utils.floor(playerPosition.x) >> CHUNK_SIZE_BITS;
        int playerChunkY = Utils.floor(playerPosition.y) >> CHUNK_SIZE_BITS;
        int playerChunkZ = Utils.floor(playerPosition.z) >> CHUNK_SIZE_BITS;
        submitTasks(playerChunkX, playerChunkY, playerChunkZ, NONE, PRE_GAME_WORLD_GENERATION_DISTANCE - 1);
        waitUntilHalt(false);
    }

    public void restart(int direction) {
        Vector3f playerPosition = ServerLogic.getPlayer().getCamera().getPosition();
        int playerChunkX = Utils.floor(playerPosition.x) >> CHUNK_SIZE_BITS;
        int playerChunkY = Utils.floor(playerPosition.y) >> CHUNK_SIZE_BITS;
        int playerChunkZ = Utils.floor(playerPosition.z) >> CHUNK_SIZE_BITS;
        synchronized (executor) {
            executor.getQueue().clear();
        }
        executor.getQueue().clear();
        ServerLogic.unloadChunks(playerChunkX, playerChunkY, playerChunkZ);

        submitTasks(playerChunkX, playerChunkY, playerChunkZ, direction, RENDER_DISTANCE_XZ);
    }

    public void waitUntilHalt(boolean haltImmediately) {
        if (haltImmediately) executor.getQueue().clear();
        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            System.err.println("Crashed when awaiting termination");
            e.printStackTrace();
        }
    }

    public void cleanUp() {
        executor.getQueue().clear();
        executor.shutdown();
    }

    private void submitTasks(int playerChunkX, int playerChunkY, int playerChunkZ, int travelDirection, int maxDistance) {
        if (!executor.isShutdown() && columnRequiresGeneration(playerChunkX, playerChunkY, playerChunkZ))
            executor.submit(new Generator(playerChunkX, playerChunkY, playerChunkZ));
        for (int ring = 1; ring <= maxDistance + 1; ring++) {

            for (int chunkX = -ring; chunkX < ring && !executor.isShutdown(); chunkX++)
                if (columnRequiresGeneration(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ))
                    executor.submit(new Generator(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ));

            for (int chunkZ = ring; chunkZ > -ring && !executor.isShutdown(); chunkZ--)
                if (columnRequiresGeneration(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ))
                    executor.submit(new Generator(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ));

            for (int chunkX = ring; chunkX > -ring && !executor.isShutdown(); chunkX--)
                if (columnRequiresGeneration(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ))
                    executor.submit(new Generator(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ));

            for (int chunkZ = -ring; chunkZ < ring && !executor.isShutdown(); chunkZ++)
                if (columnRequiresGeneration(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ))
                    executor.submit(new Generator(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ));

            if (ring == 1 && !executor.isShutdown()) {
                if (columnRequiresMeshing(playerChunkX, playerChunkY, playerChunkZ))
                    executor.submit(new MeshHandler(playerChunkX, playerChunkY, playerChunkZ, travelDirection));
                continue;
            }
            int meshRing = ring - 1;
            for (int chunkX = -meshRing; chunkX < meshRing && !executor.isShutdown(); chunkX++)
                if (columnRequiresMeshing(chunkX + playerChunkX, playerChunkY, meshRing + playerChunkZ))
                    executor.submit(new MeshHandler(chunkX + playerChunkX, playerChunkY, meshRing + playerChunkZ, travelDirection));

            for (int chunkZ = meshRing; chunkZ > -meshRing && !executor.isShutdown(); chunkZ--)
                if (columnRequiresMeshing(meshRing + playerChunkX, playerChunkY, chunkZ + playerChunkZ))
                    executor.submit(new MeshHandler(meshRing + playerChunkX, playerChunkY, chunkZ + playerChunkZ, travelDirection));

            for (int chunkX = meshRing; chunkX > -meshRing && !executor.isShutdown(); chunkX--)
                if (columnRequiresMeshing(chunkX + playerChunkX, playerChunkY, -meshRing + playerChunkZ))
                    executor.submit(new MeshHandler(chunkX + playerChunkX, playerChunkY, -meshRing + playerChunkZ, travelDirection));

            for (int chunkZ = -meshRing; chunkZ < meshRing && !executor.isShutdown(); chunkZ++)
                if (columnRequiresMeshing(-meshRing + playerChunkX, playerChunkY, chunkZ + playerChunkZ))
                    executor.submit(new MeshHandler(-meshRing + playerChunkX, playerChunkY, chunkZ + playerChunkZ, travelDirection));
        }
    }

    private boolean columnRequiresGeneration(int chunkX, int playerChunkY, int chunkZ) {
        for (int chunkY = playerChunkY + RENDER_DISTANCE_Y + 1; chunkY >= playerChunkY - RENDER_DISTANCE_Y - 1; chunkY--) {
            Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ);
            if (chunk == null || !chunk.isGenerated()) return true;
        }
        return false;
    }

    private boolean columnRequiresMeshing(int chunkX, int playerChunkY, int chunkZ) {
        for (int chunkY = playerChunkY + RENDER_DISTANCE_Y; chunkY >= playerChunkY - RENDER_DISTANCE_Y; chunkY--) {
            Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ);
            if (chunk == null || !chunk.isMeshed()) return true;
        }
        return false;
    }

    private final ThreadPoolExecutor executor;

    private record Generator(int chunkX, int playerChunkY, int chunkZ) implements Runnable {

        @Override
        public void run() {

            GenerationData generationData = new GenerationData(chunkX, chunkZ);

            for (int chunkY = playerChunkY + RENDER_DISTANCE_Y + 1; chunkY >= playerChunkY - RENDER_DISTANCE_Y - 1; chunkY--) {
                try {
                    final long expectedId = Utils.getChunkId(chunkX, chunkY, chunkZ);
                    Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ);

                    if (chunk == null) {
                        chunk = FileManager.getChunk(expectedId);
                        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ);

                        Chunk.storeChunk(chunk);
                    } else if (chunk.ID != expectedId) {
                        System.err.println("found chunk has wrong id");
                        ServerLogic.addToUnloadChunk(chunk);

                        chunk = FileManager.getChunk(expectedId);
                        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ);

                        Chunk.storeChunk(chunk);
                    }
                    if (!chunk.isGenerated()) {
                        WorldGeneration.generate(chunk, generationData);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    System.err.println("Generator:");
                    System.err.println(exception.getClass());
                    System.err.println(chunkX + " " + chunkY + " " + chunkZ);
                }
            }
        }

    }

    private record MeshHandler(int chunkX, int playerChunkY, int chunkZ, int travelDirection) implements Runnable {

        @Override
        public void run() {

            MeshGenerator meshGenerator = new MeshGenerator();

            for (int chunkY = playerChunkY + RENDER_DISTANCE_Y; chunkY >= playerChunkY - RENDER_DISTANCE_Y; chunkY--) {
                try {
                    Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ);
                    if (chunk == null) {
                        System.err.println("to mesh chunk is null");
                        System.err.println(chunkX + " " + chunkY + " " + chunkZ);
                        continue;
                    }
                    if (!chunk.isGenerated()) {
                        System.err.println("to mesh chunk hasn't been generated");
                        System.err.println(chunkX + " " + chunkY + " " + chunkZ);
                        WorldGeneration.generate(chunk);
                    }
                    if (chunk.isMeshed()) continue;
                    meshChunk(meshGenerator, chunk);

                } catch (Exception exception) {
                    System.err.println("Meshing:");
                    System.err.println(exception.getClass());
                    exception.printStackTrace();
                    System.err.println(chunkX + " " + chunkY + " " + chunkZ);
                }
            }
        }

        private void meshChunk(MeshGenerator meshGenerator, Chunk chunk) {
            meshGenerator.setChunk(chunk);
            meshGenerator.generateMesh();
            ServerLogic.addToBufferChunk(chunk);
        }
    }
}
