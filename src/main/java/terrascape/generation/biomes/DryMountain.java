package terrascape.generation.biomes;

import terrascape.generation.GenerationData;
import terrascape.utils.Utils;

import static terrascape.generation.WorldGeneration.*;

public final class DryMountain extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.getTotalX(inChunkX);
        int totalY = data.getTotalY(inChunkY);
        int totalZ = data.getTotalZ(inChunkZ);

        if (totalY > data.height) return false;

        int dirtHeight = Utils.floor(data.feature * 32 + WATER_LEVEL);
        int floorMaterialDepth = 48 - (data.steepness >> 1) + (int) (data.feature * 4.0);

        if (totalY > data.height - floorMaterialDepth && data.height <= dirtHeight)
            data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingDirtType(totalX, totalY, totalZ));
        else return false;
        return true;
    }
}
