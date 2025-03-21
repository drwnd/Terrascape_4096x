package terrascape.dataStorage.octree;

public final class HomogenousSegment extends ChunkSegment {

    HomogenousSegment(byte material, byte depth) {
        super(depth);
        this.material = material;
    }

    @Override
    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        return material;
    }

    @Override
    public ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        if (this.material == material) return this;
        if (depth < 2) return new DetailSegment(this.material, depth).storeMaterial(inChunkX, inChunkY, inChunkZ, material);
        return new SplitterSegment(this.material, depth).storeMaterial(inChunkX, inChunkY, inChunkZ, material);
    }

    @Override
    public int getByteSize() {
        return 2;
    }

    @Override
    int getType() {
        return HOMOGENOUS;
    }

    private final byte material;
}
