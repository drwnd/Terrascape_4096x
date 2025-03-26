package terrascape.generation.biomes;

import terrascape.generation.GenerationData;
import terrascape.utils.Utils;

import static terrascape.generation.WorldGeneration.*;
import static terrascape.utils.Constants.SNOW;

public final class SnowyMountain extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.getTotalX(inChunkX);
        int totalY = data.getTotalY(inChunkY);
        int totalZ = data.getTotalZ(inChunkZ);

        if (totalY > data.height) return false;

        int iceHeight = Utils.floor(data.feature * 32 + ICE_LEVEL);
        int floorMaterialDepth = 48 - (data.steepness >> 1) + (int) (data.feature * 4.0);

        if (totalY < data.height - floorMaterialDepth) return false;   // Stone placed by caller
        if (totalY > iceHeight)
            data.store(inChunkX, inChunkY, inChunkZ, getGeneratingIceType(totalX, totalY, totalZ));
        else data.store(inChunkX, inChunkY, inChunkZ, SNOW);
        return true;
    }

    @Override
    public void genSurroundingStructures(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {

    }
}
