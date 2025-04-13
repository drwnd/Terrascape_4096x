package terrascape.dataStorage.octree;

import java.util.ArrayList;

import static terrascape.utils.Constants.*;

public abstract class ChunkSegment {

    public static final byte HOMOGENOUS = 0;
    static final byte DETAIL = 1;
    static final byte SPLITTER = 2;

    public static ChunkSegment parse(byte[] bytes, int startIndex) {
        return switch (bytes[startIndex]) {
            case DETAIL -> DetailSegment.parseDetail(bytes, startIndex);
            case HOMOGENOUS -> HomogenousSegment.parseHomogenous(bytes, startIndex);
            case SPLITTER -> SplitterSegment.parseSplitter(bytes, startIndex);
            default -> {
                System.err.println("not any of them. " + " value:" + bytes[startIndex]);
                yield null;
            }
        };
    }

    public static ChunkSegment getCompressedMaterials(int x, int y, int z, byte depth, byte[] uncompressedMaterials) {
        if (isHomogenous(x, y, z, depth, uncompressedMaterials)) return new HomogenousSegment(uncompressedMaterials[getUncompressedIndex(x, y, z)]);

        if (depth < 2) {
            DetailSegment segment = new DetailSegment();
            for (int inSegmentX = 0; inSegmentX < 4; inSegmentX++)
                for (int inSegmentY = 0; inSegmentY < 4; inSegmentY++)
                    for (int inSegmentZ = 0; inSegmentZ < 4; inSegmentZ++) {
                        byte material = uncompressedMaterials[getUncompressedIndex(x + inSegmentX, y + inSegmentY, z + inSegmentZ)];
                        segment.storeNoChecks(inSegmentX, inSegmentY, inSegmentZ, material);
                    }
            return segment;
        }

        int size = 1 << depth;
        ChunkSegment segment0 = getCompressedMaterials(x, y, z, (byte) (depth - 1), uncompressedMaterials);
        ChunkSegment segment1 = getCompressedMaterials(x, y, z + size, (byte) (depth - 1), uncompressedMaterials);
        ChunkSegment segment2 = getCompressedMaterials(x, y + size, z, (byte) (depth - 1), uncompressedMaterials);
        ChunkSegment segment3 = getCompressedMaterials(x, y + size, z + size, (byte) (depth - 1), uncompressedMaterials);
        ChunkSegment segment4 = getCompressedMaterials(x + size, y, z, (byte) (depth - 1), uncompressedMaterials);
        ChunkSegment segment5 = getCompressedMaterials(x + size, y, z + size, (byte) (depth - 1), uncompressedMaterials);
        ChunkSegment segment6 = getCompressedMaterials(x + size, y + size, z, (byte) (depth - 1), uncompressedMaterials);
        ChunkSegment segment7 = getCompressedMaterials(x + size, y + size, z + size, (byte) (depth - 1), uncompressedMaterials);

        return new SplitterSegment(segment0, segment1, segment2, segment3, segment4, segment5, segment6, segment7);
    }

    public static int getUncompressedIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY;
    }

    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        return getMaterial(inChunkX, inChunkY, inChunkZ, CHUNK_SIZE_BITS - 1);
    }

    public ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size) {
        return storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, CHUNK_SIZE_BITS - 1);
    }

    public abstract int getByteSize();

    public abstract void addBytes(ArrayList<Byte> bytes);

    public abstract byte getType();

    abstract byte getMaterial(int inChunkX, int inChunkY, int inChunkZ, int depth);

    abstract ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size, int depth);

    private static boolean isHomogenous(int x, int y, int z, byte depth, byte[] uncompressedMaterials) {
        int size = 1 << depth + 1;
        byte material = uncompressedMaterials[getUncompressedIndex(x, y, z)];
        for (int inChunkX = x; inChunkX < x + size; inChunkX++)
            for (int inChunkY = y; inChunkY < y + size; inChunkY++)
                for (int inChunkZ = z; inChunkZ < z + size; inChunkZ++)
                    if (uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)] != material) return false;
        return true;
    }
}
