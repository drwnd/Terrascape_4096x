package terrascape.generation.biomes;

import terrascape.generation.GenerationData;

import static terrascape.generation.WorldGeneration.*;
import static terrascape.utils.Constants.SAND;

public final class WarmOcean extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.getTotalX(inChunkX);
        int totalY = data.getTotalY(inChunkY);
        int totalZ = data.getTotalZ(inChunkZ);

        if (totalY > data.height) return false;

        int sandHeight = (int) (data.feature * 64.0) + WATER_LEVEL - 80;
        int floorMaterialDepth = 48 - (data.steepness >> 1) + (int) (data.feature * 4.0);

        if (totalY < data.height - floorMaterialDepth) return false;   // Stone placed by caller
        if (totalY > sandHeight) data.store(inChunkX, inChunkY, inChunkZ, SAND);
        else data.store(inChunkX, inChunkY, inChunkZ, data.getWarmOceanFloorMaterial(totalX, totalY, totalZ));
        return true;
    }

    @Override
    public void genSurroundingStructures(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {

    }
}
