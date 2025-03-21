package terrascape.dataStorage.octree;

public abstract class ChunkSegment {

    static final int HOMOGENOUS = 0;
    static final int DETAIL = 1;
    static final int SPLITTER = 2;

    final byte depth;

    protected ChunkSegment(byte depth) {
        this.depth = depth;
    }

    public abstract byte getMaterial(int inChunkX, int inChunkY, int inChunkZ);

    public abstract ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material);

    public abstract int getByteSize();

    abstract int getType();
}
