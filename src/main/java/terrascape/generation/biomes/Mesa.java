package terrascape.generation.biomes;

import terrascape.generation.GenerationData;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Constants.RED_SAND;

public final class Mesa extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalY = data.chunk.Y << CHUNK_SIZE_BITS | inChunkY;

        if (totalY > data.height) return false;

        int floorMaterialDepth = 3 - (data.steepness >> 1) + (int) (data.feature * 4.0);

        if (totalY < data.height - floorMaterialDepth - 5) return false;   // Stone placed by caller
        if (totalY < data.height - floorMaterialDepth) data.chunk.storeSave(inChunkX, inChunkY, inChunkZ, RED_SANDSTONE);
        else data.chunk.storeSave(inChunkX, inChunkY, inChunkZ, RED_SAND);
        return true;
    }

    @Override
    public void genSurroundingStructures(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {

    }
}
