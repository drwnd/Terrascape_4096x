package terrascape.server;

import terrascape.entity.Structure;
import terrascape.generation.WorldGeneration;
import terrascape.player.Player;
import org.joml.Vector2f;
import org.joml.Vector3f;
import terrascape.utils.Utils;

import java.io.*;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

public final class FileManager {

    private static void loadUniversalFiles() {
        seedFile = new File("Saves/" + SEED);
        if (!seedFile.exists()) //noinspection ResultOfMethodCallIgnored
            seedFile.mkdirs();

        chunksFile = new File(seedFile.getPath() + "/chunks");
        if (!chunksFile.exists()) //noinspection ResultOfMethodCallIgnored
            chunksFile.mkdirs();
    }

    private static void generateHigherLODs() {
        for (int lod = 1; lod < LOD_COUNT; lod++) generateLod(lod);
    }

    private static void generateLod(int lod) {
        int lowerLOD = lod - 1;
        File lowerLodFile = new File(chunksFile.getPath() + "/" + lowerLOD);
        File thisLodFile = new File(chunksFile.getPath() + "/" + lod);
        if (!lowerLodFile.exists()) return;
        if (!thisLodFile.exists()) thisLodFile.mkdir();
        File[] lowerLodChunkFiles = lowerLodFile.listFiles();
        if (lowerLodChunkFiles == null) {
            System.err.println("Error occurred when listing lod " + lowerLOD + " chunk files.");
            return;
        }

        for (File chunkFile : lowerLodChunkFiles) {
            Chunk chunk = getChunk(chunkFile, lowerLOD);
            if (chunk == null) continue;
            int thisLodChunkX = chunk.X >> 1;
            int thisLodChunkY = chunk.Y >> 1;
            int thisLodChunkZ = chunk.Z >> 1;
            long thisLodChunkId = Utils.getChunkId(thisLodChunkX, thisLodChunkY, thisLodChunkZ);
            File thisLodChunkFile = new File(thisLodFile.getPath() + "/" + thisLodChunkId);
            if (thisLodChunkFile.exists()) continue;

            Chunk thisLodChunk = new Chunk(thisLodChunkX, thisLodChunkY, thisLodChunkZ, lod);
            generateChunk(thisLodChunk);
            saveChunk(thisLodChunk);
        }
        System.out.println("Finished generating lod " + lod);
    }

    private static void generateChunk(Chunk chunk) {
        WorldGeneration.generate(chunk);

        int lowLODStartX = chunk.X << 1;
        int lowLODStartY = chunk.Y << 1;
        int lowLODStartZ = chunk.Z << 1;
        int lowLOD = chunk.LOD - 1;

        Chunk chunk0 = getChunk(Utils.getChunkId(lowLODStartX, lowLODStartY, lowLODStartZ), lowLOD);
        Chunk chunk1 = getChunk(Utils.getChunkId(lowLODStartX, lowLODStartY, lowLODStartZ + 1), lowLOD);
        Chunk chunk2 = getChunk(Utils.getChunkId(lowLODStartX, lowLODStartY + 1, lowLODStartZ), lowLOD);
        Chunk chunk3 = getChunk(Utils.getChunkId(lowLODStartX, lowLODStartY + 1, lowLODStartZ + 1), lowLOD);
        Chunk chunk4 = getChunk(Utils.getChunkId(lowLODStartX + 1, lowLODStartY, lowLODStartZ), lowLOD);
        Chunk chunk5 = getChunk(Utils.getChunkId(lowLODStartX + 1, lowLODStartY, lowLODStartZ + 1), lowLOD);
        Chunk chunk6 = getChunk(Utils.getChunkId(lowLODStartX + 1, lowLODStartY + 1, lowLODStartZ), lowLOD);
        Chunk chunk7 = getChunk(Utils.getChunkId(lowLODStartX + 1, lowLODStartY + 1, lowLODStartZ + 1), lowLOD);

        chunk.storeLowerLODChunks(chunk0, chunk1, chunk2, chunk3, chunk4, chunk5, chunk6, chunk7);
    }

    public static void init() {
        loadUniversalFiles();
        generateHigherLODs();
    }

    public static BufferedReader getSettingsReader() throws Exception{
        File settings = new File("textData/Settings");
        if (!settings.exists()) throw new FileNotFoundException("Need to have settings file");

        return new BufferedReader(new FileReader(settings.getPath()));
    }

    public static void saveChunk(Chunk chunk) {
        chunk.setSaved();
        try {
            File lodFile = new File(chunksFile.getPath() + "/" + chunk.LOD);
            if (!lodFile.exists()) //noinspection ResultOfMethodCallIgnored
                lodFile.mkdir();

            File chunkFile = new File(lodFile.getPath() + "/" + chunk.ID);
            if (!chunkFile.exists()) //noinspection ResultOfMethodCallIgnored
                chunkFile.mkdir();

            saveMaterials(chunk, chunkFile);

        } catch (IOException e) {
            System.err.println("Error when saving chunk to file");
            e.printStackTrace();
        }
    }

    public static Chunk getChunk(long id, int lod) {
        File chunkFile = new File(chunksFile.getPath() + "/" + lod + "/" + id);
        if (!chunkFile.exists()) return null;

        return getChunk(chunkFile, lod);
    }

    private static Chunk getChunk(File chunkFile, int lod) {
        byte[] materialsData;
        try {
            materialsData = getMaterialsData(chunkFile);
        } catch (IOException e) {
            System.err.println("Error when reading chunk from file");
            e.printStackTrace();
            return null;
        }

        int chunkX = Utils.getInt(materialsData, 0);
        int chunkY = Utils.getInt(materialsData, 4);
        int chunkZ = Utils.getInt(materialsData, 8);
        MaterialsData materials = MaterialsData.loadFromDiscBytes(materialsData, 12);

        Chunk chunk = new Chunk(chunkX, chunkY, chunkZ, lod, materials);
        chunk.setGenerated();
        chunk.setSaved();

        return chunk;
    }

    public static void saveAllModifiedChunks() {
        for (Chunk chunk : Chunk.getWorld(0)) {
            if (chunk == null) continue;
            if (chunk.isModified()) saveChunk(chunk);
        }
    }

    public static void deleteHigherLodData() {
        for (int lod = 1; lod < LOD_COUNT; lod++) {
            boolean successfullyDeleted = deleteRecursive(new File(chunksFile.getPath() + "/" + lod));
            if (!successfullyDeleted) System.err.println("Error when deleting lod " + lod);
        }
    }

    public static void savePlayer() {
        File playerFile = new File(seedFile.getPath() + "/player");

        try {
            if (!playerFile.exists())
                //noinspection ResultOfMethodCallIgnored
                playerFile.createNewFile();

            FileOutputStream writer = new FileOutputStream(playerFile.getPath());

            Player player = ServerLogic.getPlayer();
            if (player != null) {
                Vector3f playerPosition = player.getCamera().getPosition();
                Vector2f playerRotation = player.getCamera().getRotation();
                Vector3f playerVelocity = player.getMovement().getVelocity();

                writer.write(Utils.toByteArray(Float.floatToIntBits(playerPosition.x)));
                writer.write(Utils.toByteArray(Float.floatToIntBits(playerPosition.y)));
                writer.write(Utils.toByteArray(Float.floatToIntBits(playerPosition.z)));

                writer.write(Utils.toByteArray(Float.floatToIntBits(playerRotation.x)));
                writer.write(Utils.toByteArray(Float.floatToIntBits(playerRotation.y)));

                writer.write(Utils.toByteArray(Float.floatToIntBits(playerVelocity.x)));
                writer.write(Utils.toByteArray(Float.floatToIntBits(playerVelocity.y)));
                writer.write(Utils.toByteArray(Float.floatToIntBits(playerVelocity.z)));

                writer.write(player.getMovement().getMovementState());
                writer.write(player.getSelectedHotBarSlot());
                writer.write(player.getMovement().isFlying() ? 1 : 0);
                writer.write(player.getHotBar());

                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Player loadPlayer() throws Exception {
        Player player;

        player = new Player();
        player.init();
        player.getRenderer().init();

        File playerFile = new File(seedFile.getPath() + "/player");
        if (!playerFile.exists()) return player;

        FileInputStream reader = new FileInputStream(playerFile.getPath());
        byte[] data = reader.readAllBytes();
        reader.close();

        if (data.length == 0) return player;

        float[] floats = readPlayerState(data);
        byte[] playerFlags = readPlayerFlags(data);
        byte[] hotBar = readHotBar(data);

        player.getCamera().setPosition(floats[PLAYER_X], floats[PLAYER_Y], floats[PLAYER_Z]);
        player.getCamera().setRotation(floats[PLAYER_PITCH], floats[PLAYER_YAW]);
        player.getMovement().setVelocity(floats[PLAYER_VELOCITY_X], floats[PLAYER_VELOCITY_Y], floats[PLAYER_VELOCITY_Z]);
        player.setHotBar(hotBar);
        player.getMovement().setMovementState(playerFlags[MOVEMENT_STATE]);
        player.setSelectedHotBarSlot(playerFlags[SELECTED_HOT_BAR_SLOT]);
        player.getMovement().setFlying(playerFlags[IS_FLYING] == 1);

        return player;
    }


    public static void loadGameState() throws IOException {
        File stateFile = new File(seedFile.getPath() + "/gameState");
        if (!stateFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            stateFile.createNewFile();
            return;
        }

        FileInputStream reader = new FileInputStream(stateFile.getPath());
        byte[] data = reader.readAllBytes();
        reader.close();

        if (data.length != 12) return;

        long currentTick = Utils.getLong(data, 0);
        float currentTime = Float.intBitsToFloat(Utils.getInt(data, 8));

        EngineManager.setTick(currentTick);
        ServerLogic.getPlayer().getRenderer().setTime(currentTime);
    }

    public static void saveGameState() {
        Player player = ServerLogic.getPlayer();
        if (player == null || player.getRenderer() == null) return;

        File stateFile = new File(seedFile.getPath() + "/gameState");
        try {
            if (!stateFile.exists())
                //noinspection ResultOfMethodCallIgnored
                stateFile.createNewFile();

            FileOutputStream writer = new FileOutputStream(stateFile.getPath());

            writer.write(Utils.toByteArray(EngineManager.getTick()));
            writer.write(Utils.toByteArray(Float.floatToIntBits(player.getRenderer().getTime())));

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public static Structure loadStructure(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) return null;

        FileInputStream reader = new FileInputStream(file.getPath());
        byte[] data = reader.readAllBytes();
        reader.close();

        int sizeXZ = Utils.getInt(data, 0);
        int sizeY = Utils.getInt(data, 4);
        int totalSize = sizeXZ * sizeXZ * sizeY;

        if (data.length != totalSize + 8) return null;

        byte[] materials = new byte[totalSize];
        System.arraycopy(data, 8, materials, 0, totalSize);
        return new Structure(sizeXZ, sizeY, materials);
    }


    public static void loadNames() throws Exception {

        File allMaterialTypeNames = new File("textData/MaterialNames");
        if (!allMaterialTypeNames.exists())
            throw new FileNotFoundException("Need to have all material type names file");
        BufferedReader reader = new BufferedReader(new FileReader(allMaterialTypeNames.getPath()));
        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++)
            Material.setMaterialName(material, reader.readLine());
    }


    private static void saveMaterials(Chunk chunk, File chunkFile) throws IOException {
        File materialsFile = new File(chunkFile.getPath() + "/materials");

        if (!materialsFile.exists()) //noinspection ResultOfMethodCallIgnored
            materialsFile.createNewFile();

        FileOutputStream writer = new FileOutputStream(materialsFile.getPath());
        writer.write(Utils.toByteArray(chunk.X));
        writer.write(Utils.toByteArray(chunk.Y));
        writer.write(Utils.toByteArray(chunk.Z));

        writer.write(chunk.materialsToBytes());

        writer.close();
    }

    private static float[] readPlayerState(byte[] bytes) {
        float[] floats = new float[8];

        for (int i = 0; i < floats.length; i++) {
            int index = i << 2;
            int intFloat = ((int) bytes[index] & 0xFF) << 24 | ((int) bytes[index + 1] & 0xFF) << 16 | ((int) bytes[index + 2] & 0xFF) << 8 | ((int) bytes[index + 3] & 0xFF);
            floats[i] = Float.intBitsToFloat(intFloat);
        }

        return floats;
    }

    private static byte[] readPlayerFlags(byte[] bytes) {
        byte[] flags = new byte[3];

        System.arraycopy(bytes, 32, flags, 0, flags.length);

        return flags;
    }

    private static byte[] readHotBar(byte[] bytes) {
        byte[] hotBar = new byte[9];
        System.arraycopy(bytes, 35, hotBar, 0, 9);
        return hotBar;
    }

    private static byte[] getMaterialsData(File chunkFile) throws IOException {
        FileInputStream reader = new FileInputStream(chunkFile.getPath() + "/materials");
        byte[] materialsData = reader.readAllBytes();
        reader.close();
        return materialsData;
    }

    private static boolean deleteRecursive(File file) {
        if (!file.exists()) {
            System.err.println(file.getPath() + " does not exist");
            return false;
        }
        if (file.isFile()) return file.delete();

        File[] files = file.listFiles();
        if (files == null) {
            System.err.println(file.getPath() + " does not have sub files");
            return false;
        }
        for (File subFile : files) {
            if (!deleteRecursive(subFile)) return false;
        }
        return file.delete();
    }

    private static final int PLAYER_X = 0;
    private static final int PLAYER_Y = 1;
    private static final int PLAYER_Z = 2;
    private static final int PLAYER_PITCH = 3;
    private static final int PLAYER_YAW = 4;
    private static final int PLAYER_VELOCITY_X = 5;
    private static final int PLAYER_VELOCITY_Y = 6;
    private static final int PLAYER_VELOCITY_Z = 7;
    private static final int MOVEMENT_STATE = 0;
    private static final int SELECTED_HOT_BAR_SLOT = 1;
    private static final int IS_FLYING = 2;

    private static File seedFile;
    private static File chunksFile;

    private FileManager() {
    }
}
