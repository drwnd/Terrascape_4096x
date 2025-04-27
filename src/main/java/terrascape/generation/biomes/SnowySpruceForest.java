package terrascape.generation.biomes;

import terrascape.dataStorage.Structure;
import terrascape.generation.GenerationData;
import terrascape.generation.Tree;

import static terrascape.utils.Constants.*;

public final class SnowySpruceForest extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalY = data.getTotalY(inChunkY);


        if (totalY > data.height) return false;

        int floorMaterialDepth = 48 - (data.steepness >> 1) + (int) (data.feature * 4.0);

        if (totalY < data.height - floorMaterialDepth) return false;   // Stone placed by caller
        data.store(inChunkX, inChunkY, inChunkZ, SNOW);
        return true;
    }

    @Override
    public Tree getGeneratingTree(int totalX, int totalZ, int height) {
        return getRandomTree(totalX, height, totalZ, Structure.SPRUCE_TREES);
    }
}
