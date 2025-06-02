package terrascape.dataStorage.octree;

import terrascape.entity.Light;

import java.util.ArrayList;

import static terrascape.utils.Constants.*;

final class SplitterSegment extends ChunkSegment {

    SplitterSegment(ChunkSegment segment0, ChunkSegment segment1,
                    ChunkSegment segment2, ChunkSegment segment3,
                    ChunkSegment segment4, ChunkSegment segment5,
                    ChunkSegment segment6, ChunkSegment segment7) {
        this.segment0 = segment0;
        this.segment1 = segment1;
        this.segment2 = segment2;
        this.segment3 = segment3;
        this.segment4 = segment4;
        this.segment5 = segment5;
        this.segment6 = segment6;
        this.segment7 = segment7;
    }

    SplitterSegment(byte material) {
        segment0 = new HomogenousSegment(material);
        segment1 = new HomogenousSegment(material);
        segment2 = new HomogenousSegment(material);
        segment3 = new HomogenousSegment(material);
        segment4 = new HomogenousSegment(material);
        segment5 = new HomogenousSegment(material);
        segment6 = new HomogenousSegment(material);
        segment7 = new HomogenousSegment(material);
    }

    private SplitterSegment() {

    }

    static SplitterSegment parseSplitter(byte[] bytes, int startIndex) {
        SplitterSegment segment = new SplitterSegment();
        startIndex += 1;

        segment.segment0 = ChunkSegment.parse(bytes, startIndex);
        if (segment.segment0 == null) return null;
        startIndex += segment.segment0.getDiscByteSize();

        segment.segment1 = ChunkSegment.parse(bytes, startIndex);
        if (segment.segment1 == null) return null;
        startIndex += segment.segment1.getDiscByteSize();

        segment.segment2 = ChunkSegment.parse(bytes, startIndex);
        if (segment.segment2 == null) return null;
        startIndex += segment.segment2.getDiscByteSize();

        segment.segment3 = ChunkSegment.parse(bytes, startIndex);
        if (segment.segment3 == null) return null;
        startIndex += segment.segment3.getDiscByteSize();

        segment.segment4 = ChunkSegment.parse(bytes, startIndex);
        if (segment.segment4 == null) return null;
        startIndex += segment.segment4.getDiscByteSize();

        segment.segment5 = ChunkSegment.parse(bytes, startIndex);
        if (segment.segment5 == null) return null;
        startIndex += segment.segment5.getDiscByteSize();

        segment.segment6 = ChunkSegment.parse(bytes, startIndex);
        if (segment.segment6 == null) return null;
        startIndex += segment.segment6.getDiscByteSize();

        segment.segment7 = ChunkSegment.parse(bytes, startIndex);
        if (segment.segment7 == null) return null;

        return segment;
    }

    @Override
    byte getMaterial(int inChunkX, int inChunkY, int inChunkZ, int depth) {
        int index = (inChunkX >> depth & 1) << 2 | (inChunkY >> depth & 1) << 1 | (inChunkZ >> depth & 1);
        return switch (index & 7) {
            case 0 -> segment0.getMaterial(inChunkX, inChunkY, inChunkZ, depth - 1);
            case 1 -> segment1.getMaterial(inChunkX, inChunkY, inChunkZ, depth - 1);
            case 2 -> segment2.getMaterial(inChunkX, inChunkY, inChunkZ, depth - 1);
            case 3 -> segment3.getMaterial(inChunkX, inChunkY, inChunkZ, depth - 1);
            case 4 -> segment4.getMaterial(inChunkX, inChunkY, inChunkZ, depth - 1);
            case 5 -> segment5.getMaterial(inChunkX, inChunkY, inChunkZ, depth - 1);
            case 6 -> segment6.getMaterial(inChunkX, inChunkY, inChunkZ, depth - 1);
            case 7 -> segment7.getMaterial(inChunkX, inChunkY, inChunkZ, depth - 1);
            default -> OUT_OF_WORLD; // Mathematically unreachable
        };
    }

    @Override
    ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size, int depth) {
        if (size >= depth + 1) return new HomogenousSegment(material);

        int index = (inChunkX >> depth & 1) << 2 | (inChunkY >> depth & 1) << 1 | (inChunkZ >> depth & 1);
        switch (index & 7) {
            case 0 -> segment0 = segment0.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth - 1);
            case 1 -> segment1 = segment1.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth - 1);
            case 2 -> segment2 = segment2.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth - 1);
            case 3 -> segment3 = segment3.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth - 1);
            case 4 -> segment4 = segment4.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth - 1);
            case 5 -> segment5 = segment5.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth - 1);
            case 6 -> segment6 = segment6.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth - 1);
            case 7 -> segment7 = segment7.storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth - 1);
        }

        if (segment0.getType() == HOMOGENOUS && segment0.getMaterial(0, 0, 0, depth - 1) == material &&
                segment1.getType() == HOMOGENOUS && segment1.getMaterial(0, 0, 0, depth - 1) == material &&
                segment2.getType() == HOMOGENOUS && segment2.getMaterial(0, 0, 0, depth - 1) == material &&
                segment3.getType() == HOMOGENOUS && segment3.getMaterial(0, 0, 0, depth - 1) == material &&
                segment4.getType() == HOMOGENOUS && segment4.getMaterial(0, 0, 0, depth - 1) == material &&
                segment5.getType() == HOMOGENOUS && segment5.getMaterial(0, 0, 0, depth - 1) == material &&
                segment6.getType() == HOMOGENOUS && segment6.getMaterial(0, 0, 0, depth - 1) == material &&
                segment7.getType() == HOMOGENOUS && segment7.getMaterial(0, 0, 0, depth - 1) == material) {
            return new HomogenousSegment(material);
        }
        return this;
    }

    @Override
    public int getDiscByteSize() {
        return 1
                + segment0.getDiscByteSize() + segment1.getDiscByteSize() + segment2.getDiscByteSize() + segment3.getDiscByteSize()
                + segment4.getDiscByteSize() + segment5.getDiscByteSize() + segment6.getDiscByteSize() + segment7.getDiscByteSize();
    }

    @Override
    public int getRAMByteSize() {
        return 32 + 16
                + segment0.getRAMByteSize() + segment1.getRAMByteSize() + segment2.getRAMByteSize() + segment3.getRAMByteSize()
                + segment4.getRAMByteSize() + segment5.getRAMByteSize() + segment6.getRAMByteSize() + segment7.getRAMByteSize();
    }

    @Override
    public void addBytes(ArrayList<Byte> bytes) {
        bytes.add(SPLITTER);
        segment0.addBytes(bytes);
        segment1.addBytes(bytes);
        segment2.addBytes(bytes);
        segment3.addBytes(bytes);
        segment4.addBytes(bytes);
        segment5.addBytes(bytes);
        segment6.addBytes(bytes);
        segment7.addBytes(bytes);
    }

    @Override
    byte getType() {
        return SPLITTER;
    }

    @Override
    void addLights(int x, int y, int z, int depth, int lod, ArrayList<Light> lights) {
        int nextSize = 1 << depth;
        segment0.addLights(x, y, z, depth - 1, lod, lights);
        segment1.addLights(x, y, z + nextSize, depth - 1, lod, lights);
        segment2.addLights(x, y + nextSize, z, depth - 1, lod, lights);
        segment3.addLights(x, y + nextSize, z + nextSize, depth - 1, lod, lights);
        segment4.addLights(x + nextSize, y, z, depth - 1, lod, lights);
        segment5.addLights(x + nextSize, y, z + nextSize, depth - 1, lod, lights);
        segment6.addLights(x + nextSize, y + nextSize, z, depth - 1, lod, lights);
        segment7.addLights(x + nextSize, y + nextSize, z + nextSize, depth - 1, lod, lights);
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
