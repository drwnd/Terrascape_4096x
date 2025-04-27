package terrascape.generation.biomes;

import terrascape.dataStorage.Structure;
import terrascape.generation.GenerationData;
import terrascape.generation.Tree;

import static terrascape.utils.Constants.CHUNK_SIZE_BITS;

public abstract class Biome {

    public abstract boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data);

    public Tree getGeneratingTree(int totalX, int totalZ, int height) {
        return null;
    }

    public String getName() {
        return getClass().getName();
    }

    public int getSpecialHeight(int totalX, int totalZ, GenerationData data) {
        return 0;
    }

    protected Tree getRandomTree(int x, int y, int z, Structure[] trees) {
        return new Tree(x, z, y, trees[Math.abs(x + y + z >> CHUNK_SIZE_BITS) % trees.length], (byte) (y & 7));
    }
}