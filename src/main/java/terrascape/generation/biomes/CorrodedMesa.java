package terrascape.generation.biomes;

import terrascape.generation.GenerationData;
import terrascape.generation.OpenSimplex2S;

import static terrascape.generation.WorldGeneration.*;
import static terrascape.utils.Constants.*;
import static terrascape.utils.Constants.RED_SAND;
import static terrascape.utils.Settings.SEED;

public final class CorrodedMesa extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalY = data.getTotalY(inChunkY);

        int pillarHeight = data.specialHeight;
        int floorMaterialDepth = 48 - (data.steepness >> 1) + (int) (data.feature * 4.0);
        if (pillarHeight != 0 && totalY >= data.height - floorMaterialDepth) {
            if (totalY > data.height + pillarHeight) return false;
            data.store(inChunkX, inChunkY, inChunkZ, getGeneratingTerracottaType(totalY >> 4 & 15));
            return true;
        }

        if (totalY > data.height) return false;

        if (totalY < data.height - floorMaterialDepth - 80) return false;   // Stone placed by caller
        if (totalY < data.height - floorMaterialDepth) data.store(inChunkX, inChunkY, inChunkZ, RED_SANDSTONE);
        else data.store(inChunkX, inChunkY, inChunkZ, RED_SAND);
        return true;
    }

    @Override
    public void genSurroundingStructures(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {

    }

    @Override
    public int getSpecialHeight(int totalX, int totalZ, GenerationData data) {
        double noise = OpenSimplex2S.noise2(SEED ^ 0xDF860F2E2A604A17L, totalX * MESA_PILLAR_FREQUENCY, totalZ * MESA_PILLAR_FREQUENCY);
        if (Math.abs(noise) > MESA_PILLAR_THRESHOLD) return MESA_PILLAR_HEIGHT;
        return 0;
    }
}
