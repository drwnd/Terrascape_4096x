package terrascape.generation.biomes;

import terrascape.dataStorage.Structure;
import terrascape.generation.GenerationData;
import terrascape.generation.Tree;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Constants.DIRT;

public abstract class Biome {

    public abstract boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data);


    public Tree getGeneratingTree(int totalX, int totalZ, int height) {
        return null;
    }

    public int getSpecialHeight(int totalX, int totalZ, GenerationData data) {
        return 0;
    }

    public int getRequiredTreeZeroBits() {
        return 0;
    }

    public final String getName() {
        return getClass().getName();
    }


    protected static Tree getRandomTree(int x, int y, int z, Structure[] trees) {
        return new Tree(x, z, y, trees[Math.abs(x + y + z >> CHUNK_SIZE_BITS) % trees.length], (byte) (y & 7));
    }

    protected static boolean placeHomogenousSurfaceMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data, byte material) {
        int totalY = data.getTotalY(inChunkY);

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        data.store(inChunkX, inChunkY, inChunkZ, material);
        return true;
    }

    protected static boolean placeLayeredSurfaceMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data, byte topMaterial) {
        int totalY = data.getTotalY(inChunkY);

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (data.isInsideSurfaceMaterialLevel(totalY, 8)) data.store(inChunkX, inChunkY, inChunkZ, topMaterial);
        else data.store(inChunkX, inChunkY, inChunkZ, DIRT);
        return true;
    }
}