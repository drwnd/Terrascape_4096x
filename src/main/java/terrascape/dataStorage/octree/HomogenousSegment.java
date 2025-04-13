package terrascape.dataStorage.octree;

import java.util.ArrayList;

public final class HomogenousSegment extends ChunkSegment {

    public HomogenousSegment(byte material) {
        this.material = material;
    }

    static HomogenousSegment parseHomogenous(byte[] bytes, int startIndex) {
        return new HomogenousSegment(bytes[startIndex + 1]);
    }

    @Override
    byte getMaterial(int inChunkX, int inChunkY, int inChunkZ, int depth) {
        return material;
    }

    @Override
    ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size, int depth) {
        if (this.material == material) return this;
        if (depth < 2)
            return new DetailSegment(this.material).storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth);
        return new SplitterSegment(this.material).storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth);
    }

    @Override
    public int getByteSize() {
        return 2;
    }

    @Override
    public void addBytes(ArrayList<Byte> bytes) {
        bytes.add(HOMOGENOUS);
        bytes.add(material);
    }

    @Override
    public byte getType() {
        return HOMOGENOUS;
    }

    private final byte material;
}
