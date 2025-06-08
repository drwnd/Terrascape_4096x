package terrascape.entity;

import terrascape.server.FileManager;

import java.io.IOException;

public final class Structure {

    public static final byte MIRROR_X = 1;
    public static final byte MIRROR_Z = 2;
    public static final byte ROTATE_90 = 4;

    public static final Structure[] OAK_TREES = new Structure[10];
    public static final Structure[] SPRUCE_TREES = new Structure[10];
    public static final Structure[] DARK_OAK_TREES = new Structure[10];
    public static final Structure[] PINE_TREES = new Structure[10];
    public static final Structure[] REDWOOD_TREES = new Structure[10];
    public static final Structure[] BLACK_WOOD_TREES = new Structure[10];

    public final int sizeXZ, sizeY;

    public static void init() throws IOException {
        for (int i = 0; i < 10; i++) {
            OAK_TREES[i] = FileManager.loadStructure("structures/oakTree" + i);
            SPRUCE_TREES[i] = FileManager.loadStructure("structures/spruceTree" + i);
            DARK_OAK_TREES[i] = FileManager.loadStructure("structures/darkOakTree" + i);
            PINE_TREES[i] = FileManager.loadStructure("structures/pineTree" + i);
            REDWOOD_TREES[i] = FileManager.loadStructure("structures/redwoodTree" + i);
            BLACK_WOOD_TREES[i] = FileManager.loadStructure("structures/blackWoodTree" + i);

            if (OAK_TREES[i] == null) System.err.println("oak" + i);
            if (SPRUCE_TREES[i] == null) System.err.println("spruce" + i);
            if (DARK_OAK_TREES[i] == null) System.err.println("dark oak" + i);
            if (PINE_TREES[i] == null) System.err.println("pine" + i);
            if (REDWOOD_TREES[i] == null) System.err.println("redwood" + i);
            if (BLACK_WOOD_TREES[i] == null) System.err.println("black wood" + i);
        }
    }

    public Structure(int sizeXZ, int sizeY, byte[] materials) {
        this.sizeXZ = sizeXZ;
        this.sizeY = sizeY;
        this.materials = materials;
    }

    public byte getMaterial(int structureX, int structureY, int structureZ, byte transform) {
        if ((transform & ROTATE_90) != 0) {
            int temp = sizeXZ - structureX - 1;
            structureX = structureZ;
            structureZ = temp;
        }
        if ((transform & MIRROR_X) != 0) structureX = sizeXZ - structureX - 1;
        if ((transform & MIRROR_Z) != 0) structureZ = sizeXZ - structureZ - 1;

        return materials[getIndex(structureX, structureY, structureZ)];
    }

    private int getIndex(int structureX, int structureY, int structureZ) {
        return (structureX * sizeXZ + structureZ) * sizeY + structureY;
    }

    private final byte[] materials;
}
