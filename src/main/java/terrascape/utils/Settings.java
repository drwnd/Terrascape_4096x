package terrascape.utils;

import org.lwjgl.glfw.GLFW;
import terrascape.entity.GUIElement;
import terrascape.player.Player;
import terrascape.server.FileManager;
import terrascape.server.ServerLogic;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

public final class Settings {
    public static final int IS_MOUSE_BUTTON = 0x80000000;
    public static final int IS_KEYBOARD_BUTTON = 0;

    //Change to whatever you want
//        public static final long SEED = new Random().nextLong();
    public static long SEED;

    // Values read from file
    public static float FOV;
    public static float GUI_SIZE;
    public static float MOUSE_SENSITIVITY;
    public static float REACH;
    public static float TEXT_SIZE;

    public static int MOVE_FORWARD_BUTTON;
    public static int MOVE_BACK_BUTTON;
    public static int MOVE_RIGHT_BUTTON;
    public static int MOVE_LEFT_BUTTON;

    public static int JUMP_BUTTON;
    public static int SPRINT_BUTTON;
    public static int SNEAK_BUTTON;
    public static int CRAWL_BUTTON;
    public static int FLY_FAST_BUTTON;

    public static int HOT_BAR_SLOT_1;
    public static int HOT_BAR_SLOT_2;
    public static int HOT_BAR_SLOT_3;
    public static int HOT_BAR_SLOT_4;
    public static int HOT_BAR_SLOT_5;
    public static int HOT_BAR_SLOT_6;
    public static int HOT_BAR_SLOT_7;
    public static int HOT_BAR_SLOT_8;
    public static int HOT_BAR_SLOT_9;

    public static int DESTROY_BUTTON;
    public static int USE_BUTTON;
    public static int PICK_MATERIAL_BUTTON;

    public static int OPEN_INVENTORY_BUTTON;
    public static int OPEN_DEBUG_MENU_BUTTON;
    public static int TOGGLE_X_RAY_BUTTON;
    public static int TOGGLE_NO_CLIP_BUTTON;
    public static int RELOAD_SETTINGS_BUTTON;
    public static int ZOOM_BUTTON;
    public static float AUDIO_GAIN;
    public static float STEP_GAIN;
    public static float PLACE_GAIN;
    public static float DIG_GAIN;
    public static float MISCELLANEOUS_GAIN;
    public static float INVENTORY_GAIN;
    public static int RELOAD_SHADERS_BUTTON;
    public static int INCREASE_BREAK_PLACE_SIZE_BUTTON;
    public static int DECREASE_BREAK_PLACE_SIZE_BUTTON;
    public static int DROP_BUTTON;
    public static boolean RAW_MOUSE_INPUT;
    public static boolean SCROLL_HOT_BAR;
    public static boolean DO_SHADOW_MAPPING;

    // Calculated values

    public static int TEXT_CHAR_SIZE_X;
    public static int TEXT_CHAR_SIZE_Y;
    public static int TEXT_LINE_SPACING;

    public static void init() throws Exception {
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

        loadSettings(true);
    }

    public static void loadSettings() throws Exception {
        loadSettings(false);
    }

    private static void loadSettings(boolean initialLoad) throws Exception {
        BufferedReader reader = FileManager.getSettingsReader();

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
        DO_SHADOW_MAPPING = Boolean.parseBoolean(getStingAfterColon(reader.readLine()));

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
        } else if (ServerLogic.getPlayer() != null) {
            ServerLogic.getPlayer().updateSettings();
        }
    }


    private static String getStingAfterColon(String string) {
        return string.substring(string.indexOf(':') + 1);
    }

    private static final Map<String, Integer> KEY_CODES = new HashMap<>(70);


    private Settings() {
    }
}
