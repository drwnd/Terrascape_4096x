package terrascape.server;

import terrascape.player.SoundManager;

import static terrascape.utils.Constants.*;

public final class Material {

    public static final byte[][] NORMALS = {{0, 0, 1}, {0, 1, 0}, {1, 0, 0}, {0, 0, -1}, {0, -1, 0}, {-1, 0, 0}};

    public static int getTextureIndex(byte material, int side) {
        byte[] materialTextureIndices;
        materialTextureIndices = STANDARD_BLOCK_TEXTURE_INDICES[material & 0xFF];
        return materialTextureIndices[side >= materialTextureIndices.length ? 0 : side];
    }

    public static byte getToPlaceMaterial(byte toPlaceMaterial) {
        return toPlaceMaterial;
    }

    public static int getMaterialProperties(byte material) {
        return STANDARD_BLOCK_PROPERTIES[material & 0xFF];
    }

    public static int[] getDigSound(byte material) {
        return STANDARD_BLOCK_DIG_SOUNDS[material & 0xFF];
    }

    public static int[] getFootstepsSound(byte material) {
        return STANDARD_BLOCK_STEP_SOUNDS[material & 0xFF];
    }

    public static boolean isWaterMaterial(byte material) {
        return material == WATER;
    }

    public static boolean isLeaveType(byte material) {
        return material >= OAK_LEAVES && material <= BLACK_WOOD_LEAVES;
    }

    public static void setStandardMaterialName(int index, String name) {
        STANDARD_BLOCK_NAMES[index] = name;
    }

    public static String getMaterialName(byte material) {
        return STANDARD_BLOCK_NAMES[material & 0xFF];
    }

    private static void setMaterialData(byte material, int properties, int[] digSounds, int[] stepSounds, byte[] textures) {
        if (textures != null) STANDARD_BLOCK_TEXTURE_INDICES[(material & 0xFF)] = textures;
        STANDARD_BLOCK_PROPERTIES[(material & 0xFF)] = properties;
        if (digSounds != null) STANDARD_BLOCK_DIG_SOUNDS[(material & 0xFF)] = digSounds;
        if (stepSounds != null) STANDARD_BLOCK_STEP_SOUNDS[(material & 0xFF)] = stepSounds;
    }

    private static void setMaterialData(byte material, int properties, int[] digSounds, int[] stepSounds, byte texture) {
        STANDARD_BLOCK_TEXTURE_INDICES[(material & 0xFF)] = new byte[]{texture};
        STANDARD_BLOCK_PROPERTIES[(material & 0xFF)] = properties;
        if (digSounds != null) STANDARD_BLOCK_DIG_SOUNDS[(material & 0xFF)] = digSounds;
        if (stepSounds != null) STANDARD_BLOCK_STEP_SOUNDS[(material & 0xFF)] = stepSounds;
    }


    private static void initMaterials() {
        SoundManager sound = Launcher.getSound();

        setMaterialData(AIR, NO_COLLISION | REPLACEABLE, null, null, (byte) 0);
        setMaterialData(OUT_OF_WORLD, 0, null, null, (byte) 0);
        setMaterialData(PATH_BLOCK, 0, sound.digGrass, sound.stepGrass, new byte[]{(byte) -72, (byte) -88, (byte) -72, (byte) -72, (byte) 1, (byte) -72});
        setMaterialData(CACTUS, REQUIRES_BOTTOM_SUPPORT, sound.digWood, sound.stepWood, new byte[]{(byte) 113, (byte) -92, (byte) 113, (byte) 113, (byte) -92, (byte) 113});

        setMaterialData(WATER, NO_COLLISION | REPLACEABLE | BLAST_RESISTANT, sound.splash, sound.splash, new byte[]{(byte) 64});
        setMaterialData(LAVA, NO_COLLISION | REPLACEABLE | BLAST_RESISTANT | LIGHT_LEVEL_15 | HAS_ASKEW_FACES, sound.lavaPop, sound.lavaPop, new byte[]{(byte) -127});

        setMaterialData(GRASS, 0, sound.digGrass, sound.stepGrass, new byte[]{(byte) 0xBA, (byte) 0xAA, (byte) 0xBA, (byte) 0xBA, (byte) 1, (byte) 0xBA});
        setMaterialData(DIRT, 0, sound.digGrass, sound.stepDirt, (byte) 1);
        setMaterialData(STONE, 0, sound.digStone, sound.stepStone, (byte) 2);
        setMaterialData(STONE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) 34);
        setMaterialData(COBBLESTONE, 0, sound.digStone, sound.stepStone, (byte) 50);
        setMaterialData(CHISELED_STONE, 0, sound.digStone, sound.stepStone, (byte) 81);
        setMaterialData(POLISHED_STONE, 0, sound.digStone, sound.stepStone, (byte) 66);
        setMaterialData(CHISELED_POLISHED_STONE, 0, sound.digStone, sound.stepStone, (byte) 82);
        setMaterialData(MUD, 0, sound.digGrass, sound.stepDirt, (byte) 17);
        setMaterialData(ANDESITE, 0, sound.digStone, sound.stepStone, (byte) 18);
        setMaterialData(SNOW, 0, sound.digSnow, sound.stepSnow, (byte) 32);
        setMaterialData(SAND, HAS_GRAVITY, sound.digSand, sound.stepSand, (byte) 33);
        setMaterialData(SANDSTONE, 0, sound.digStone, sound.stepStone, (byte) -95);
        setMaterialData(POLISHED_SANDSTONE, 0, sound.digStone, sound.stepStone, (byte) -94);
        setMaterialData(SLATE, 0, sound.digStone, sound.stepStone, (byte) 48);
        setMaterialData(CHISELED_SLATE, 0, sound.digStone, sound.stepStone, (byte) -128);
        setMaterialData(COBBLED_SLATE, 0, sound.digStone, sound.stepStone, (byte) 114);
        setMaterialData(SLATE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) -126);
        setMaterialData(POLISHED_SLATE, 0, sound.digStone, sound.stepStone, (byte) -96);
        setMaterialData(GLASS, 0, sound.digGlass, sound.stepGlass, (byte) 49);
        setMaterialData(GRAVEL, HAS_GRAVITY, sound.digGravel, sound.stepGravel, (byte) 65);
        setMaterialData(COURSE_DIRT, 0, sound.digGrass, sound.stepDirt, (byte) 80);
        setMaterialData(CLAY, 0, sound.digGrass, sound.stepDirt, (byte) 97);
        setMaterialData(MOSS, 0, sound.digFoliage, sound.stepFoliage, (byte) 98);
        setMaterialData(ICE, 0, sound.digIce, sound.stepGlass, (byte) 96);
        setMaterialData(HEAVY_ICE, 0, sound.digIce, sound.stepGlass, (byte) 112);
        setMaterialData(COAL_ORE, 0, sound.digStone, sound.stepStone, (byte) -112);
        setMaterialData(IRON_ORE, 0, sound.digStone, sound.stepStone, (byte) -111);
        setMaterialData(DIAMOND_ORE, 0, sound.digStone, sound.stepStone, (byte) -110);

        setMaterialData(OAK_LOG, 0, sound.digWood, sound.stepWood, (byte) 3);
        setMaterialData(STRIPPED_OAK_LOG, 0, sound.digWood, sound.stepWood, (byte) 35);
        setMaterialData(SPRUCE_LOG, 0, sound.digWood, sound.stepWood, (byte) 4);
        setMaterialData(STRIPPED_SPRUCE_LOG, 0, sound.digWood, sound.stepWood, (byte) 36);
        setMaterialData(DARK_OAK_LOG, 0, sound.digWood, sound.stepWood, (byte) 5);
        setMaterialData(STRIPPED_DARK_OAK_LOG, 0, sound.digWood, sound.stepWood, (byte) 37);
        setMaterialData(PINE_LOG, 0, sound.digWood, sound.stepWood, (byte) 6);
        setMaterialData(STRIPPED_PINE_LOG, 0, sound.digWood, sound.stepWood, (byte) 38);
        setMaterialData(REDWOOD_LOG, 0, sound.digWood, sound.stepWood, (byte) 7);
        setMaterialData(STRIPPED_REDWOOD_LOG, 0, sound.digWood, sound.stepWood, (byte) 39);
        setMaterialData(BLACK_WOOD_LOG, 0, sound.digWood, sound.stepWood, (byte) 8);
        setMaterialData(STRIPPED_BLACK_WOOD_LOG, 0, sound.digWood, sound.stepWood, (byte) 40);
        setMaterialData(BASALT, 0, sound.stepStone, sound.digStone, (byte) 0x89);

        setMaterialData(OAK_LEAVES, 0, sound.digFoliage, sound.stepFoliage, (byte) 83);
        setMaterialData(SPRUCE_LEAVES, 0, sound.digFoliage, sound.stepFoliage, (byte) 84);
        setMaterialData(DARK_OAK_LEAVES, 0, sound.digFoliage, sound.stepFoliage, (byte) 85);
        setMaterialData(PINE_LEAVES, 0, sound.digFoliage, sound.stepFoliage, (byte) 86);
        setMaterialData(REDWOOD_LEAVES, 0, sound.digFoliage, sound.stepFoliage, (byte) 87);
        setMaterialData(BLACK_WOOD_LEAVES, 0, sound.digFoliage, sound.stepFoliage, (byte) 88);
        setMaterialData(OAK_PLANKS, 0, sound.digWood, sound.stepWood, (byte) 67);
        setMaterialData(SPRUCE_PLANKS, 0, sound.digWood, sound.stepWood, (byte) 68);
        setMaterialData(DARK_OAK_PLANKS, 0, sound.digWood, sound.stepWood, (byte) 69);
        setMaterialData(PINE_PLANKS, 0, sound.digWood, sound.stepWood, (byte) 70);
        setMaterialData(REDWOOD_PLANKS, 0, sound.digWood, sound.stepWood, (byte) 71);
        setMaterialData(BLACK_WOOD_PLANKS, 0, sound.digWood, sound.stepWood, (byte) 72);
        setMaterialData(CRACKED_ANDESITE, 0, sound.digStone, sound.stepStone, (byte) -93);
        setMaterialData(BLACK, 0, sound.digStone, sound.stepStone, (byte) -9);
        setMaterialData(WHITE, 0, sound.digStone, sound.stepStone, (byte) -8);
        setMaterialData(CYAN, 0, sound.digStone, sound.stepStone, (byte) -7);
        setMaterialData(MAGENTA, 0, sound.digStone, sound.stepStone, (byte) -6);
        setMaterialData(YELLOW, 0, sound.digStone, sound.stepStone, (byte) -5);
        setMaterialData(BLUE, 0, sound.digStone, sound.stepStone, (byte) -4);
        setMaterialData(GREEN, 0, sound.digStone, sound.stepStone, (byte) -3);
        setMaterialData(RED, 0, sound.digStone, sound.stepStone, (byte) -2);
        setMaterialData(CRAFTING_TABLE, INTERACTABLE, sound.digWood, sound.stepWood, new byte[]{(byte) -78, (byte) -79, (byte) -77, (byte) -78, (byte) 68, (byte) -77});
        setMaterialData(TNT, INTERACTABLE, sound.digGrass, sound.stepGrass, new byte[]{(byte) -106, (byte) -122, (byte) -106, (byte) -106, (byte) -90, (byte) -106});
        setMaterialData(OBSIDIAN, BLAST_RESISTANT, sound.digStone, sound.stepStone, (byte) -76);
        setMaterialData(MOSSY_STONE, 0, sound.digStone, sound.stepStone, (byte) -17);
        setMaterialData(MOSSY_ANDESITE, 0, sound.digStone, sound.stepStone, (byte) -18);
        setMaterialData(MOSSY_STONE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) -19);
        setMaterialData(MOSSY_POLISHED_STONE, 0, sound.digStone, sound.stepStone, (byte) -20);
        setMaterialData(MOSSY_CHISELED_POLISHED_STONE, 0, sound.digStone, sound.stepStone, (byte) -21);
        setMaterialData(MOSSY_CHISELED_STONE, 0, sound.digStone, sound.stepStone, (byte) -22);
        setMaterialData(MOSSY_SLATE, 0, sound.digStone, sound.stepStone, (byte) -23);
        setMaterialData(MOSSY_COBBLED_SLATE, 0, sound.digStone, sound.stepStone, (byte) -24);
        setMaterialData(MOSSY_SLATE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) -25);
        setMaterialData(MOSSY_CHISELED_SLATE, 0, sound.digStone, sound.stepStone, (byte) -26);
        setMaterialData(MOSSY_POLISHED_SLATE, 0, sound.digStone, sound.stepStone, (byte) -27);
        setMaterialData(MOSSY_POLISHED_SANDSTONE, 0, sound.digStone, sound.stepStone, (byte) 0xE4);
        setMaterialData(MOSSY_SANDSTONE, 0, sound.digStone, sound.stepStone, (byte) 0xE3);
        setMaterialData(MOSSY_OBSIDIAN, BLAST_RESISTANT, sound.digStone, sound.stepStone, (byte) 0xE2);
        setMaterialData(MOSSY_CRACKED_ANDESITE, 0, sound.digStone, sound.stepStone, (byte) 0xE1);
        setMaterialData(MOSSY_COBBLESTONE, 0, sound.digStone, sound.stepStone, (byte) 0xE0);
        setMaterialData(SEA_LIGHT, LIGHT_LEVEL_15, sound.digGlass, sound.stepGlass, (byte) -120);
        setMaterialData(PODZOL, 0, sound.digGrass, sound.stepGrass, new byte[]{(byte) 0xB9, (byte) 0xA9, (byte) 0xB9, (byte) 0xB9, (byte) 0x01, (byte) 0xB9});
        setMaterialData(RED_SAND, HAS_GRAVITY, sound.digSand, sound.stepSand, (byte) 0xBE);
        setMaterialData(RED_POLISHED_SANDSTONE, 0, sound.digStone, sound.stepStone, (byte) 0xBD);
        setMaterialData(RED_SANDSTONE, 0, sound.digStone, sound.stepStone, (byte) 0xBC);
        setMaterialData(TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xDF);
        setMaterialData(RED_TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xDE);
        setMaterialData(GREEN_TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xDD);
        setMaterialData(BLUE_TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xDC);
        setMaterialData(YELLOW_TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xDB);
        setMaterialData(MAGENTA_TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xDA);
        setMaterialData(CYAN_TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xD9);
        setMaterialData(WHITE_TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xD8);
        setMaterialData(BLACK_TERRACOTTA, 0, sound.digStone, sound.stepStone, (byte) 0xD7);
        setMaterialData(RED_WOOL, 0, sound.digCloth, sound.stepCloth, (byte) 0xCE);
        setMaterialData(GREEN_WOOL, 0, sound.digCloth, sound.stepCloth, (byte) 0xCD);
        setMaterialData(BLUE_WOOL, 0, sound.digCloth, sound.stepCloth, (byte) 0xCC);
        setMaterialData(YELLOW_WOOL, 0, sound.digCloth, sound.stepCloth, (byte) 0xCB);
        setMaterialData(MAGENTA_WOOL, 0, sound.digCloth, sound.stepCloth, (byte) 0xCA);
        setMaterialData(CYAN_WOOL, 0, sound.digCloth, sound.stepCloth, (byte) 0xC9);
        setMaterialData(WHITE_WOOL, 0, sound.digCloth, sound.stepCloth, (byte) 0xC8);
        setMaterialData(BLACK_WOOL, 0, sound.digCloth, sound.stepCloth, (byte) 0xC7);
        setMaterialData(MOSSY_SANDSTONE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) 0xF0);
        setMaterialData(MOSSY_RED_SANDSTONE, 0, sound.digStone, sound.stepStone, (byte) 0xF1);
        setMaterialData(MOSSY_RED_POLISHED_SANDSTONE, 0, sound.digStone, sound.stepStone, (byte) 0xF2);
        setMaterialData(MOSSY_RED_SANDSTONE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) 0xF3);
        setMaterialData(SANDSTONE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) 0xB6);
        setMaterialData(RED_SANDSTONE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) 0xBF);
        setMaterialData(COBBLED_BLACKSTONE, 0, sound.digStone, sound.stepStone, (byte) 0x73);
        setMaterialData(BLACKSTONE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) 0x74);
        setMaterialData(POLISHED_BLACKSTONE, 0, sound.digStone, sound.stepStone, (byte) 0x75);
        setMaterialData(COAL_BLOCK, 0, sound.digStone, sound.stepStone, (byte) 0x99);
        setMaterialData(IRON_BLOCK, 0, sound.digStone, sound.stepStone, (byte) 0x9A);
        setMaterialData(DIAMOND_BLOCK, 0, sound.digStone, sound.stepStone, (byte) 0x9B);
        setMaterialData(MOSSY_COBBLED_BLACKSTONE, 0, sound.digStone, sound.stepStone, (byte) 0xF4);
        setMaterialData(MOSSY_BLACKSTONE_BRICKS, 0, sound.digStone, sound.stepStone, (byte) 0xF5);
        setMaterialData(MOSSY_POLISHED_BLACKSTONE, 0, sound.digStone, sound.stepStone, (byte) 0x9C);
        setMaterialData(BLACKSTONE, 0, sound.digStone, sound.stepStone, (byte) 0x76);
        setMaterialData(MOSSY_BLACKSTONE, 0, sound.digStone, sound.stepStone, (byte) 0x77);
    }

    //I don't know how to use JSON-Files, so just ignore it
    public static void init() {
        initMaterials();
    }

    private static final byte[][] STANDARD_BLOCK_TEXTURE_INDICES = new byte[AMOUNT_OF_STANDARD_BLOCKS][1];
    private static final int[] STANDARD_BLOCK_PROPERTIES = new int[AMOUNT_OF_STANDARD_BLOCKS];

    private static final int[][] STANDARD_BLOCK_DIG_SOUNDS = new int[AMOUNT_OF_STANDARD_BLOCKS][0];
    private static final int[][] STANDARD_BLOCK_STEP_SOUNDS = new int[AMOUNT_OF_STANDARD_BLOCKS][0];

    private static final String[] STANDARD_BLOCK_NAMES = new String[256];

    private Material() {
    }
}
