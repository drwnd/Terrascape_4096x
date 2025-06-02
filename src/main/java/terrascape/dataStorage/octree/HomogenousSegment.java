package terrascape.dataStorage.octree;

import terrascape.entity.Light;
import terrascape.server.Material;

import java.awt.*;
import java.util.ArrayList;

import static terrascape.utils.Constants.CHUNK_SIZE_BITS;
import static terrascape.utils.Constants.EMITS_LIGHT;

final class HomogenousSegment extends ChunkSegment {

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
        if (depth < 2) return new DetailSegment(this.material).storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth);
        return new SplitterSegment(this.material).storeMaterial(inChunkX, inChunkY, inChunkZ, material, size, depth);
    }

    @Override
    public int getDiscByteSize() {
        return 2;
    }

    @Override
    public int getRAMByteSize() {
        return 16;
    }

    @Override
    public void addBytes(ArrayList<Byte> bytes) {
        bytes.add(HOMOGENOUS);
        bytes.add(material);
    }

    @Override
    byte getType() {
        return HOMOGENOUS;
    }

    @Override
    void addLights(int x, int y, int z, int depth, int lod, ArrayList<Light> lights) {
        if ((Material.getMaterialProperties(material) & EMITS_LIGHT) == 0) return;
        Color light = Material.getMaterialLight(material);

        x += 1 << depth;
        y += 1 << depth;
        z += 1 << depth;
        int strength = depth + lod + 1 << 24;

        lights.add(new Light(strength | x << CHUNK_SIZE_BITS * 2 | y << CHUNK_SIZE_BITS | z, light.getRGB()));
    }

    private final byte material;
}
