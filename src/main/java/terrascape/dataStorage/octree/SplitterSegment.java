package terrascape.dataStorage.octree;

import static terrascape.utils.Constants.*;

public final class SplitterSegment extends ChunkSegment {

    SplitterSegment(byte material, byte depth) {
        super(depth);
        segment0 = new HomogenousSegment(material, (byte) (depth - 1));
        segment1 = new HomogenousSegment(material, (byte) (depth - 1));
        segment2 = new HomogenousSegment(material, (byte) (depth - 1));
        segment3 = new HomogenousSegment(material, (byte) (depth - 1));
        segment4 = new HomogenousSegment(material, (byte) (depth - 1));
        segment5 = new HomogenousSegment(material, (byte) (depth - 1));
        segment6 = new HomogenousSegment(material, (byte) (depth - 1));
        segment7 = new HomogenousSegment(material, (byte) (depth - 1));
    }

    @Override
    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        int index = (inChunkX >> depth & 1) << 2 | (inChunkY >> depth & 1) << 1 | (inChunkZ >> depth & 1);
        return switch (index & 7) {
            case 0 -> segment0.getMaterial(inChunkX, inChunkY, inChunkZ);
            case 1 -> segment1.getMaterial(inChunkX, inChunkY, inChunkZ);
            case 2 -> segment2.getMaterial(inChunkX, inChunkY, inChunkZ);
            case 3 -> segment3.getMaterial(inChunkX, inChunkY, inChunkZ);
            case 4 -> segment4.getMaterial(inChunkX, inChunkY, inChunkZ);
            case 5 -> segment5.getMaterial(inChunkX, inChunkY, inChunkZ);
            case 6 -> segment6.getMaterial(inChunkX, inChunkY, inChunkZ);
            case 7 -> segment7.getMaterial(inChunkX, inChunkY, inChunkZ);
            default -> OUT_OF_WORLD; // Mathematically unreachable
        };
    }

    @Override
    public ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        int index = (inChunkX >> depth & 1) << 2 | (inChunkY >> depth & 1) << 1 | (inChunkZ >> depth & 1);
        switch (index & 7) {
            case 0 -> segment0 = segment0.storeMaterial(inChunkX, inChunkY, inChunkZ, material);
            case 1 -> segment1 = segment1.storeMaterial(inChunkX, inChunkY, inChunkZ, material);
            case 2 -> segment2 = segment2.storeMaterial(inChunkX, inChunkY, inChunkZ, material);
            case 3 -> segment3 = segment3.storeMaterial(inChunkX, inChunkY, inChunkZ, material);
            case 4 -> segment4 = segment4.storeMaterial(inChunkX, inChunkY, inChunkZ, material);
            case 5 -> segment5 = segment5.storeMaterial(inChunkX, inChunkY, inChunkZ, material);
            case 6 -> segment6 = segment6.storeMaterial(inChunkX, inChunkY, inChunkZ, material);
            case 7 -> segment7 = segment7.storeMaterial(inChunkX, inChunkY, inChunkZ, material);
        }

        if (segment0.getType() == HOMOGENOUS && segment0.getMaterial(0, 0, 0) == material &&
                segment1.getType() == HOMOGENOUS && segment1.getMaterial(0, 0, 0) == material &&
                segment2.getType() == HOMOGENOUS && segment2.getMaterial(0, 0, 0) == material &&
                segment3.getType() == HOMOGENOUS && segment3.getMaterial(0, 0, 0) == material &&
                segment4.getType() == HOMOGENOUS && segment4.getMaterial(0, 0, 0) == material &&
                segment5.getType() == HOMOGENOUS && segment5.getMaterial(0, 0, 0) == material &&
                segment6.getType() == HOMOGENOUS && segment6.getMaterial(0, 0, 0) == material &&
                segment7.getType() == HOMOGENOUS && segment7.getMaterial(0, 0, 0) == material) {
            return new HomogenousSegment(material, depth);
        }
        return this;
    }

    @Override
    public int getByteSize() {
        return 1 + segment0.getByteSize() + segment1.getByteSize() + segment2.getByteSize() + segment3.getByteSize()
                + segment4.getByteSize() + segment5.getByteSize() + segment6.getByteSize() + segment7.getByteSize();
    }

    @Override
    int getType() {
        return SPLITTER;
    }

    private ChunkSegment segment0;
    private ChunkSegment segment1;
    private ChunkSegment segment2;
    private ChunkSegment segment3;
    private ChunkSegment segment4;
    private ChunkSegment segment5;
    private ChunkSegment segment6;
    private ChunkSegment segment7;

}
