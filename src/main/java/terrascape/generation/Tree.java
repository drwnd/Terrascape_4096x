package terrascape.generation;

import terrascape.dataStorage.Structure;

public record Tree(int centerX, int centerZ, int baseY, Structure tree, byte transform) {

    public byte getMaterial(int totalX, int totalY, int totalZ) {
        return tree.getMaterial(totalX - getMinX(), totalY - baseY, totalZ - getMinZ(), transform);
    }

    public int getMinX() {
        return centerX - (tree.sizeXZ >> 1);
    }

    public int getMinY() {
        return baseY;
    }

    public int getMinZ() {
        return centerZ - (tree.sizeXZ >> 1);
    }

    public int getMaxX() {
        return centerX + (tree.sizeXZ >> 1);
    }

    public int getMaxY() {
        return baseY + tree.sizeY;
    }

    public int getMaxZ() {
        return centerZ + (tree.sizeXZ >> 1);
    }
}
