package terrascape.generation.biomes;

import terrascape.entity.Structure;
import terrascape.generation.GenerationData;
import terrascape.generation.Tree;

import static terrascape.utils.Constants.*;

public final class SnowyPlains extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        return Biome.placeHomogenousSurfaceMaterial(inChunkX, inChunkY, inChunkZ, data, SNOW);
    }

    @Override
    public Tree getGeneratingTree(int totalX, int totalZ, int height) {
        return getRandomTree(totalX, height, totalZ, Structure.SPRUCE_TREES);
    }

    @Override
    public int getRequiredTreeZeroBits() {
        return 0b01001010010010000;
    }
}
