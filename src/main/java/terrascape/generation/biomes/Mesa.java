package terrascape.generation.biomes;

import terrascape.generation.GenerationData;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Constants.RED_SAND;

public final class Mesa extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalY = data.getTotalY(inChunkY);

        if (totalY > data.height) return false;

        int floorMaterialDepth = 48 - (data.steepness >> 1) + (int) (data.feature * 4.0);

        if (totalY < data.height - floorMaterialDepth - 80) return false;   // Stone placed by caller
        if (totalY < data.height - floorMaterialDepth) data.store(inChunkX, inChunkY, inChunkZ, RED_SANDSTONE);
        else data.store(inChunkX, inChunkY, inChunkZ, RED_SAND);
        return true;
    }
}
