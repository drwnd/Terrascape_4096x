package terrascape.generation.biomes;

import terrascape.generation.GenerationData;

import static terrascape.generation.WorldGeneration.*;
import static terrascape.utils.Constants.DIRT;

public final class RedwoodForest extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.getTotalX(inChunkX);
        int totalY = data.getTotalY(inChunkY);
        int totalZ = data.getTotalZ(inChunkZ);


        if (totalY > data.height) return false;

        int floorMaterialDepth = 48 - (data.steepness >> 1) + (int) (data.feature * 4.0);

        if (totalY < data.height - floorMaterialDepth) return false;   // Stone placed by caller
        if (totalY >= data.height - 8)
            data.store(inChunkX, inChunkY, inChunkZ, getGeneratingGrassType(totalX, totalZ, data));
        else data.store(inChunkX, inChunkY, inChunkZ, DIRT);
        return true;
    }

    @Override
    public void genSurroundingStructures(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {

    }
}
