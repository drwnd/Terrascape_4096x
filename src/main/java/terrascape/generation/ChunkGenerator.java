package terrascape.generation;

import terrascape.player.Player;
import terrascape.dataStorage.octree.Chunk;
import terrascape.dataStorage.FileManager;
import terrascape.utils.Utils;
import terrascape.server.ServerLogic;
import org.joml.Vector3f;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static terrascape.utils.Constants.*;

public final class ChunkGenerator {

    public ChunkGenerator() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_GENERATION_THREADS);
    }

    public void generateSurrounding(Player player) {
        Vector3f playerPosition = player.getCamera().getPosition();
        int playerChunkX = Utils.floor(playerPosition.x) >> CHUNK_SIZE_BITS;
        int playerChunkY = Utils.floor(playerPosition.y) >> CHUNK_SIZE_BITS;
        int playerChunkZ = Utils.floor(playerPosition.z) >> CHUNK_SIZE_BITS;
        if (!executor.isShutdown() && columnRequiresGeneration(playerChunkX, playerChunkY, playerChunkZ, 0))
            executor.submit(new Generator(playerChunkX, playerChunkY, playerChunkZ, 0));
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

        submitTasks(playerChunkX, playerChunkY, playerChunkZ, direction);
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

    private void submitTasks(int playerChunkX, int playerChunkY, int playerChunkZ, int travelDirection) {
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            int lodPlayerX = playerChunkX >> lod;
            int lodPlayerY = playerChunkY >> lod;
            int lodPlayerZ = playerChunkZ >> lod;

            if (!executor.isShutdown() && columnRequiresGeneration(lodPlayerX, lodPlayerY, lodPlayerZ, lod))
                executor.submit(new Generator(lodPlayerX, lodPlayerY, lodPlayerZ, lod));

            for (int ring = 1; ring <= RENDER_DISTANCE_XZ + 1; ring++) {
                submitRingGeneration(lodPlayerX, lodPlayerY, lodPlayerZ, ring, lod);
                submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, travelDirection, ring - 2, lod);
            }
            submitRingMeshing(lodPlayerX, lodPlayerY, lodPlayerZ, travelDirection, RENDER_DISTANCE_XZ, lod);
        }
    }

    private void submitRingMeshing(int playerChunkX, int playerChunkY, int playerChunkZ, int travelDirection, int ring, int lod) {
        if (ring < 0) return;
        if (ring == 0) {
            if (columnRequiresMeshing(playerChunkX, playerChunkY, playerChunkZ, lod))
                executor.submit(new MeshHandler(playerChunkX, playerChunkY, playerChunkZ, travelDirection, lod));
            return;
        }

        for (int chunkX = -ring; chunkX < ring && !executor.isShutdown(); chunkX++)
            if (columnRequiresMeshing(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod))
                executor.submit(new MeshHandler(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, travelDirection, lod));

        for (int chunkZ = ring; chunkZ > -ring && !executor.isShutdown(); chunkZ--)
            if (columnRequiresMeshing(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod))
                executor.submit(new MeshHandler(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, travelDirection, lod));

        for (int chunkX = ring; chunkX > -ring && !executor.isShutdown(); chunkX--)
            if (columnRequiresMeshing(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod))
                executor.submit(new MeshHandler(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, travelDirection, lod));

        for (int chunkZ = -ring; chunkZ < ring && !executor.isShutdown(); chunkZ++)
            if (columnRequiresMeshing(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod))
                executor.submit(new MeshHandler(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, travelDirection, lod));
    }

    private void submitRingGeneration(int playerChunkX, int playerChunkY, int playerChunkZ, int ring, int lod) {
        for (int chunkX = -ring; chunkX < ring && !executor.isShutdown(); chunkX++)
            if (columnRequiresGeneration(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod))
                executor.submit(new Generator(chunkX + playerChunkX, playerChunkY, ring + playerChunkZ, lod));

        for (int chunkZ = ring; chunkZ > -ring && !executor.isShutdown(); chunkZ--)
            if (columnRequiresGeneration(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod))
                executor.submit(new Generator(ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod));

        for (int chunkX = ring; chunkX > -ring && !executor.isShutdown(); chunkX--)
            if (columnRequiresGeneration(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod))
                executor.submit(new Generator(chunkX + playerChunkX, playerChunkY, -ring + playerChunkZ, lod));

        for (int chunkZ = -ring; chunkZ < ring && !executor.isShutdown(); chunkZ++)
            if (columnRequiresGeneration(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod))
                executor.submit(new Generator(-ring + playerChunkX, playerChunkY, chunkZ + playerChunkZ, lod));
    }

    private boolean columnRequiresGeneration(int chunkX, int playerChunkY, int chunkZ, int lod) {
        for (int chunkY = playerChunkY + RENDER_DISTANCE_Y + 1; chunkY >= playerChunkY - RENDER_DISTANCE_Y - 1; chunkY--) {
            Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ, lod);
            if (chunk == null || !chunk.isGenerated()) return true;
        }
        return false;
    }

    private boolean columnRequiresMeshing(int chunkX, int playerChunkY, int chunkZ, int lod) {
        for (int chunkY = playerChunkY + RENDER_DISTANCE_Y; chunkY >= playerChunkY - RENDER_DISTANCE_Y; chunkY--) {
            Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ, lod);
            if (chunk == null || !chunk.isMeshed()) return true;
        }
        return false;
    }

    private final ThreadPoolExecutor executor;

    private record Generator(int chunkX, int playerChunkY, int chunkZ, int lod) implements Runnable {

        @Override
        public void run() {

            GenerationData generationData = new GenerationData(chunkX, chunkZ, lod);

            for (int chunkY = playerChunkY + RENDER_DISTANCE_Y + 1; chunkY >= playerChunkY - RENDER_DISTANCE_Y - 1; chunkY--) {
                try {
                    final long expectedId = Utils.getChunkId(chunkX, chunkY, chunkZ);
                    Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ, lod);

                    if (chunk == null) {
                        chunk = FileManager.getChunk(expectedId, lod);
                        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ, lod);

                        Chunk.storeChunk(chunk);
                    } else if (chunk.ID != expectedId) {
                        System.err.println("found chunk has wrong id found LOD:" + chunk.LOD + " expected:" + lod);
                        System.err.printf("expected %s %s %s%n", chunkX, chunkY, chunkZ);
                        System.err.printf("found    %s %s %s%n", chunk.X, chunk.Y, chunk.Z);
                        ServerLogic.addToUnloadChunk(chunk);

                        chunk = FileManager.getChunk(expectedId, lod);
                        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ, lod);

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

    private record MeshHandler(int chunkX, int playerChunkY, int chunkZ, int travelDirection,
                               int lod) implements Runnable {

        @Override
        public void run() {

            MeshGenerator meshGenerator = new MeshGenerator();
            long meshTime = 0;
            int counter = 0;

            for (int chunkY = playerChunkY + RENDER_DISTANCE_Y; chunkY >= playerChunkY - RENDER_DISTANCE_Y; chunkY--) {
                try {
                    Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ, lod);
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
                    counter++;
                    long start = System.nanoTime();
                    meshChunk(meshGenerator, chunk);
                    meshTime += System.nanoTime() - start;

                } catch (Exception exception) {
                    System.err.println("Meshing:");
                    System.err.println(exception.getClass());
                    exception.printStackTrace();
                    System.err.println(chunkX + " " + chunkY + " " + chunkZ);
                }
            }
//            System.out.printf("Meshed %s chunks in %sns average : %s%n", counter, meshTime, meshTime / counter);
        }

        private void meshChunk(MeshGenerator meshGenerator, Chunk chunk) {
            meshGenerator.setChunk(chunk);
            meshGenerator.generateMesh();
            ServerLogic.addToBufferChunk(chunk);
        }
    }
}
