package terrascape.dataStorage;

import terrascape.dataStorage.octree.Chunk;
import terrascape.dataStorage.octree.ChunkSegment;
import terrascape.entity.GUIElement;
import terrascape.generation.WorldGeneration;
import terrascape.player.Player;
import terrascape.server.EngineManager;
import terrascape.server.Material;
import terrascape.server.ServerLogic;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import terrascape.utils.Utils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

public final class FileManager {

    public static void init() {
        KEY_CODES.put("LEFT_CLICK", GLFW.GLFW_MOUSE_BUTTON_LEFT | IS_MOUSE_BUTTON);
        KEY_CODES.put("RIGHT_CLICK", GLFW.GLFW_MOUSE_BUTTON_RIGHT | IS_MOUSE_BUTTON);
        KEY_CODES.put("MIDDLE_CLICK", GLFW.GLFW_MOUSE_BUTTON_MIDDLE | IS_MOUSE_BUTTON);
        KEY_CODES.put("MOUSE_BUTTON_4", GLFW.GLFW_MOUSE_BUTTON_4 | IS_MOUSE_BUTTON);
        KEY_CODES.put("MOUSE_BUTTON_5", GLFW.GLFW_MOUSE_BUTTON_5 | IS_MOUSE_BUTTON);
        KEY_CODES.put("MOUSE_BUTTON_6", GLFW.GLFW_MOUSE_BUTTON_6 | IS_MOUSE_BUTTON);
        KEY_CODES.put("MOUSE_BUTTON_7", GLFW.GLFW_MOUSE_BUTTON_7 | IS_MOUSE_BUTTON);
        KEY_CODES.put("MOUSE_BUTTON_8", GLFW.GLFW_MOUSE_BUTTON_8 | IS_MOUSE_BUTTON);

        KEY_CODES.put("0", GLFW.GLFW_KEY_0 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("1", GLFW.GLFW_KEY_1 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("2", GLFW.GLFW_KEY_2 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("3", GLFW.GLFW_KEY_3 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("4", GLFW.GLFW_KEY_4 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("5", GLFW.GLFW_KEY_5 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("6", GLFW.GLFW_KEY_6 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("7", GLFW.GLFW_KEY_7 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("8", GLFW.GLFW_KEY_8 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("9", GLFW.GLFW_KEY_9 | IS_KEYBOARD_BUTTON);

        KEY_CODES.put("F1", GLFW.GLFW_KEY_F1 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F2", GLFW.GLFW_KEY_F2 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F3", GLFW.GLFW_KEY_F3 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F4", GLFW.GLFW_KEY_F4 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F5", GLFW.GLFW_KEY_F5 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F6", GLFW.GLFW_KEY_F6 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F7", GLFW.GLFW_KEY_F7 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F8", GLFW.GLFW_KEY_F8 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F9", GLFW.GLFW_KEY_F9 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F10", GLFW.GLFW_KEY_F10 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F11", GLFW.GLFW_KEY_F11 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F12", GLFW.GLFW_KEY_F12 | IS_KEYBOARD_BUTTON);

        KEY_CODES.put("TAB", GLFW.GLFW_KEY_TAB | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("CAPS_LOCK", GLFW.GLFW_KEY_CAPS_LOCK | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("SPACE", GLFW.GLFW_KEY_SPACE | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("LEFT_SHIFT", GLFW.GLFW_KEY_LEFT_SHIFT | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("<", GLFW.GLFW_KEY_WORLD_2 | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("LEFT_CONTROL", GLFW.GLFW_KEY_LEFT_CONTROL | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("LEFT_ALT", GLFW.GLFW_KEY_LEFT_ALT | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("RIGHT_SHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("RIGHT_CONTROL", GLFW.GLFW_KEY_RIGHT_CONTROL | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("RIGHT_ALT", GLFW.GLFW_KEY_RIGHT_ALT | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("PLUS", GLFW.GLFW_KEY_RIGHT_BRACKET);
        KEY_CODES.put("HASHTAG", GLFW.GLFW_KEY_BACKSLASH);
        KEY_CODES.put("COMMA", GLFW.GLFW_KEY_COMMA);
        KEY_CODES.put("POINT", GLFW.GLFW_KEY_PERIOD);
        KEY_CODES.put("MINUS", GLFW.GLFW_KEY_SLASH);
        KEY_CODES.put("^", GLFW.GLFW_KEY_GRAVE_ACCENT);
        KEY_CODES.put("UP", GLFW.GLFW_KEY_UP | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("LEFT", GLFW.GLFW_KEY_LEFT | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("DOWN", GLFW.GLFW_KEY_DOWN | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("RIGHT", GLFW.GLFW_KEY_RIGHT | IS_KEYBOARD_BUTTON);

        KEY_CODES.put("A", GLFW.GLFW_KEY_A | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("B", GLFW.GLFW_KEY_B | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("C", GLFW.GLFW_KEY_C | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("D", GLFW.GLFW_KEY_D | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("E", GLFW.GLFW_KEY_E | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("F", GLFW.GLFW_KEY_F | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("G", GLFW.GLFW_KEY_G | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("H", GLFW.GLFW_KEY_H | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("I", GLFW.GLFW_KEY_I | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("J", GLFW.GLFW_KEY_J | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("K", GLFW.GLFW_KEY_K | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("L", GLFW.GLFW_KEY_L | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("M", GLFW.GLFW_KEY_M | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("N", GLFW.GLFW_KEY_N | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("O", GLFW.GLFW_KEY_O | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("P", GLFW.GLFW_KEY_P | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("Q", GLFW.GLFW_KEY_Q | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("R", GLFW.GLFW_KEY_R | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("S", GLFW.GLFW_KEY_S | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("T", GLFW.GLFW_KEY_T | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("U", GLFW.GLFW_KEY_U | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("V", GLFW.GLFW_KEY_V | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("W", GLFW.GLFW_KEY_W | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("X", GLFW.GLFW_KEY_X | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("Y", GLFW.GLFW_KEY_Y | IS_KEYBOARD_BUTTON);
        KEY_CODES.put("Z", GLFW.GLFW_KEY_Z | IS_KEYBOARD_BUTTON);
    }

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
            generateChunk(lowerLodFile, thisLodChunk);
            saveChunk(thisLodChunk);
        }
        System.out.println("Finished generating lod " + lod);
    }

    private static void generateChunk(File lowerLodFile, Chunk chunk) {
        WorldGeneration.generate(chunk);

        int lowerLodChunkXStart = chunk.X << 1;
        int lowerLodChunkYStart = chunk.Y << 1;
        int lowerLodChunkZStart = chunk.Z << 1;

        for (int chunkX = lowerLodChunkXStart; chunkX <= lowerLodChunkXStart + 1; chunkX++)
            for (int chunkY = lowerLodChunkYStart; chunkY <= lowerLodChunkYStart + 1; chunkY++)
                for (int chunkZ = lowerLodChunkZStart; chunkZ <= lowerLodChunkZStart + 1; chunkZ++) {
                    long id = Utils.getChunkId(chunkX, chunkY, chunkZ);
                    File lowerLodChunkFile = new File(lowerLodFile.getPath() + "/" + id);
                    if (!lowerLodChunkFile.exists()) continue;

                    Chunk lowerLodChunk = getChunk(lowerLodChunkFile, chunk.LOD - 1);
                    if (lowerLodChunk == null) continue;

                    for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX += 2)
                        for (int inChunkY = 0; inChunkY < CHUNK_SIZE; inChunkY += 2)
                            for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ += 2) {
                                byte material = lowerLodChunk.getSaveMaterial(inChunkX, inChunkY, inChunkZ);

                                int thisChunkInChunkX = (inChunkX >> 1) + (chunkX - lowerLodChunkXStart) * (CHUNK_SIZE / 2);
                                int thisChunkInChunkY = (inChunkY >> 1) + (chunkY - lowerLodChunkYStart) * (CHUNK_SIZE / 2);
                                int thisChunkInChunkZ = (inChunkZ >> 1) + (chunkZ - lowerLodChunkZStart) * (CHUNK_SIZE / 2);
                                chunk.store(thisChunkInChunkX, thisChunkInChunkY, thisChunkInChunkZ, material);
                            }
                }
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
        ChunkSegment materials = ChunkSegment.parse(materialsData, 12);
        if (materials == null) {
            System.err.println("Failed to load materials data");
            return null;
        }

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

    public static void loadSettings(boolean initialLoad) throws Exception {
        File settings = new File("textData/Settings");
        if (!settings.exists()) throw new FileNotFoundException("Need to have settings file");

        BufferedReader reader = new BufferedReader(new FileReader(settings.getPath()));

        FOV = (float) Math.toRadians(Float.parseFloat(getStingAfterColon(reader.readLine())));
        float newGUISize = Float.parseFloat(getStingAfterColon(reader.readLine()));
        MOUSE_SENSITIVITY = Float.parseFloat(getStingAfterColon(reader.readLine()));
        REACH = Float.parseFloat(getStingAfterColon(reader.readLine()));
        TEXT_SIZE = Float.parseFloat(getStingAfterColon(reader.readLine()));

        MOVE_FORWARD_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        MOVE_BACK_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        MOVE_RIGHT_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        MOVE_LEFT_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));

        JUMP_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        SPRINT_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        SNEAK_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        CRAWL_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        FLY_FAST_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));

        HOT_BAR_SLOT_1 = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        HOT_BAR_SLOT_2 = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        HOT_BAR_SLOT_3 = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        HOT_BAR_SLOT_4 = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        HOT_BAR_SLOT_5 = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        HOT_BAR_SLOT_6 = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        HOT_BAR_SLOT_7 = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        HOT_BAR_SLOT_8 = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        HOT_BAR_SLOT_9 = KEY_CODES.get(getStingAfterColon(reader.readLine()));

        DESTROY_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        USE_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        PICK_MATERIAL_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));

        OPEN_INVENTORY_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        OPEN_DEBUG_MENU_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        TOGGLE_X_RAY_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        TOGGLE_NO_CLIP_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        RELOAD_SETTINGS_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        SCROLL_HOT_BAR = Boolean.parseBoolean(getStingAfterColon(reader.readLine()));
        ZOOM_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        AUDIO_GAIN = Float.parseFloat(getStingAfterColon(reader.readLine()));
        STEP_GAIN = Float.parseFloat(getStingAfterColon(reader.readLine()));
        PLACE_GAIN = Float.parseFloat(getStingAfterColon(reader.readLine()));
        DIG_GAIN = Float.parseFloat(getStingAfterColon(reader.readLine()));
        MISCELLANEOUS_GAIN = Float.parseFloat(getStingAfterColon(reader.readLine()));
        INVENTORY_GAIN = Float.parseFloat(getStingAfterColon(reader.readLine()));
        RELOAD_SHADERS_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        long seed = Long.parseLong(getStingAfterColon(reader.readLine()));
        INCREASE_BREAK_PLACE_SIZE_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        DECREASE_BREAK_PLACE_SIZE_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        DROP_BUTTON = KEY_CODES.get(getStingAfterColon(reader.readLine()));
        RAW_MOUSE_INPUT = Boolean.parseBoolean(getStingAfterColon(reader.readLine()));

        reader.close();
        updateSettings(newGUISize, seed, initialLoad);
    }

    private static void updateSettings(float newGUISize, long seed, boolean initialLoad) throws Exception {
        TEXT_CHAR_SIZE_X = (int) (16 * TEXT_SIZE);
        TEXT_CHAR_SIZE_Y = (int) (24 * TEXT_SIZE);
        TEXT_LINE_SPACING = (int) (28 * TEXT_SIZE);

        if (GUI_SIZE != newGUISize) {
            Player player = ServerLogic.getPlayer();
            GUI_SIZE = newGUISize;
            if (player != null) {
                GUIElement.reloadGUIElements(player);
                player.updateHotBarElements();
            }
        }

        if (initialLoad) {
            SEED = seed;
            loadUniversalFiles();
            generateHigherLODs();
        } else {
            ServerLogic.getPlayer().updateSettings();
        }
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

    private static String getStingAfterColon(String string) {
        return string.substring(string.indexOf(':') + 1);
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
        if (!file.exists()) return false;
        if (file.isFile()) return file.delete();

        File[] files = file.listFiles();
        if (files == null) return false;
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
    private static final Map<String, Integer> KEY_CODES = new HashMap<>(70);

    private FileManager() {
    }
}
