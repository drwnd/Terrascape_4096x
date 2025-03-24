package terrascape.dataStorage.octree;

import java.util.ArrayList;

public abstract class ChunkSegment {

    static final byte HOMOGENOUS = 0;
    static final byte DETAIL = 1;
    static final byte SPLITTER = 2;

    final byte depth;

    protected ChunkSegment(byte depth) {
        this.depth = depth;
    }

    public static ChunkSegment parse(byte[] bytes, int startIndex, byte depth) {
        return switch (bytes[startIndex]) {
            case DETAIL -> DetailSegment.parseDetail(bytes, startIndex, depth);
            case HOMOGENOUS -> HomogenousSegment.parseHomogenous(bytes, startIndex, depth);
            case SPLITTER -> SplitterSegment.parseSplitter(bytes, startIndex, depth);
            default -> {
                System.err.println("not any of them. depth:" + depth + " value:" + bytes[startIndex]);
                yield null;
            }
        };
    }

    public abstract byte getMaterial(int inChunkX, int inChunkY, int inChunkZ);

    public abstract ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size);

    public abstract int getByteSize();

    public abstract void addBytes(ArrayList<Byte> bytes);

    abstract byte getType();
}
