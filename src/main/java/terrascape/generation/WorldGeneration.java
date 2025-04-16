package terrascape.generation;

import terrascape.generation.biomes.*;
import terrascape.dataStorage.octree.Chunk;
import terrascape.utils.Utils;

import static terrascape.utils.Constants.*;

public final class WorldGeneration {

    public static final int WATER_LEVEL = 0;
    public static final int SNOW_LEVEL = WATER_LEVEL + 91;
    public static final int ICE_LEVEL = WATER_LEVEL + 141;

    public static final double ICE_BERG_FREQUENCY = 0.0015625;
    public static final double ICE_BERG_THRESHOLD = 0.45;
    public static final double ICE_BERG_HEIGHT = 128;
    public static final double ICE_PLANE_THRESHOLD = 0.3;

    public static final double MESA_PILLAR_THRESHOLD = 0.55;
    public static final double MESA_PILLAR_FREQUENCY = 0.001875;
    public static final int MESA_PILLAR_HEIGHT = 400;

    public static void init() {
        BIOMES[DESERT] = new Desert();
        BIOMES[WASTELAND] = new Wasteland();
        BIOMES[DARK_OAK_FOREST] = new DarkOakForest();
        BIOMES[SNOWY_SPRUCE_FOREST] = new SnowySpruceForest();
        BIOMES[SNOWY_PLAINS] = new SnowyPlains();
        BIOMES[SPRUCE_FOREST] = new SpruceForest();
        BIOMES[PLAINS] = new Plains();
        BIOMES[OAK_FOREST] = new OakForest();
        BIOMES[WARM_OCEAN] = new WarmOcean();
        BIOMES[COLD_OCEAN] = new ColdOcean();
        BIOMES[OCEAN] = new Ocean();
        BIOMES[DRY_MOUNTAIN] = new DryMountain();
        BIOMES[SNOWY_MOUNTAIN] = new SnowyMountain();
        BIOMES[MOUNTAIN] = new Mountain();
        BIOMES[MESA] = new Mesa();
        BIOMES[CORRODED_MESA] = new CorrodedMesa();
        BIOMES[BEACH] = new Beach();
        BIOMES[PINE_FOREST] = new PineForest();
        BIOMES[REDWOOD_FOREST] = new RedwoodForest();
        BIOMES[BLACK_WOOD_FOREST] = new BlackWoodForest();
    }

    public static void generate(Chunk chunk) {
        if (chunk.isGenerated()) {
            return;
        }
        generate(chunk, new GenerationData(chunk.X, chunk.Z, chunk.LOD));
    }

    public static void generate(Chunk chunk, GenerationData generationData) {
        if (chunk.isGenerated()) return;
        chunk.setGenerated();

        generationData.setChunk(chunk);

        for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX++)
            for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ++) {

                generationData.set(inChunkX, inChunkZ);
                Biome biome = BIOMES[getBiome(generationData)];
                generationData.setBiome(inChunkX, inChunkZ, biome);

                generateBiome(biome, inChunkX, inChunkZ, generationData);
            }

        chunk.setMaterials(generationData.getCompressedMaterials());
        Chunk.storeChunk(chunk);
    }

    private static void generateBiome(Biome biome, int inChunkX, int inChunkZ, GenerationData data) {
        for (int inChunkY = 0; inChunkY < CHUNK_SIZE; inChunkY++) {
            int totalY = data.getTotalY(inChunkY);

            // Attempting to place biome specific materials and features
            boolean placedMaterial = biome.placeMaterial(inChunkX, inChunkY, inChunkZ, data);

            // Placing stone beneath surface materials
            if (!placedMaterial && totalY <= data.height) {
                int totalX = data.getTotalX(inChunkX);
                int totalZ = data.getTotalZ(inChunkZ);
                data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingStoneType(totalX, totalY, totalZ));
            }

            // Filling Oceans with water
            if (totalY > data.height && totalY < WATER_LEVEL && !placedMaterial)
                data.store(inChunkX, inChunkY, inChunkZ, WATER);
        }
    }


    public static int getResultingHeight(double height, double erosion, double continental, double river, double ridge) {
        height = (height * 0.5 + 0.5) * MAX_TERRAIN_HEIGHT_DIFFERENCE;

        double continentalModifier = getContinentalModifier(continental, ridge);
        double erosionModifier = getErosionModifier(height, erosion, continentalModifier);
        double riverModifier = getRiverModifier(height, continentalModifier, erosionModifier, river);

        return Utils.floor((height + continentalModifier + erosionModifier + riverModifier) * 2) + WATER_LEVEL - 15;
    }

    private static double getContinentalModifier(double continental, double ridge) {
        double continentalModifier = 0.0;
        // Mountains
        if (continental > MOUNTAIN_THRESHOLD)
            continentalModifier = (continental - MOUNTAIN_THRESHOLD) * (continental - MOUNTAIN_THRESHOLD) * ridge * 100000;
            // Normal ocean
        else if (continental < OCEAN_THRESHOLD && continental > OCEAN_THRESHOLD - 0.05)
            continentalModifier = Utils.smoothInOutQuad(-continental, -OCEAN_THRESHOLD, -OCEAN_THRESHOLD + 0.05) * OCEAN_FLOOR_LEVEL;
        else if (continental <= OCEAN_THRESHOLD - 0.05 && continental > OCEAN_THRESHOLD - 0.2)
            continentalModifier = (continental - (OCEAN_THRESHOLD - 0.05)) * 100 + OCEAN_FLOOR_LEVEL;
            // Deep Ocean
        else if (continental <= OCEAN_THRESHOLD - 0.2 && continental > OCEAN_THRESHOLD - 0.25)
            continentalModifier = Utils.smoothInOutQuad(-continental, -OCEAN_THRESHOLD + 0.2, -OCEAN_THRESHOLD + 0.25) * DEEP_OCEAN_FLOOR_OFFSET + OCEAN_FLOOR_LEVEL - 15;
        else if (continental <= OCEAN_THRESHOLD - 0.25)
            continentalModifier = (continental - (OCEAN_THRESHOLD - 0.25)) * 100 + OCEAN_FLOOR_LEVEL + DEEP_OCEAN_FLOOR_OFFSET - 15;
        return continentalModifier;
    }

    private static double getErosionModifier(double height, double erosion, double continentalModifier) {
        double erosionModifier = 0.0;
        // Elevated areas
        if (erosion < -0.25 && erosion > -0.4) erosionModifier = Utils.smoothInOutQuad(-erosion, 0.25, 0.4) * 55;
        else if (erosion <= -0.40) erosionModifier = (erosion + 0.40) * 20 + 55;
            // Flatland
        else if (erosion > FLATLAND_THRESHOLD && erosion < FLATLAND_THRESHOLD + 0.25)
            erosionModifier = -(continentalModifier + height * 0.75 - FLATLAND_LEVEL) * Utils.smoothInOutQuad(erosion, FLATLAND_THRESHOLD, FLATLAND_THRESHOLD + 0.25);
        else if (erosion >= FLATLAND_THRESHOLD + 0.25)
            erosionModifier = -height * 0.75 - continentalModifier + FLATLAND_LEVEL;
        return erosionModifier;
    }

    private static double getRiverModifier(double height, double continentalModifier, double erosionModifier, double river) {
        double riverModifier = 0.0;
        if (Math.abs(river) < 0.005)
            riverModifier = -height * 0.85 - continentalModifier - erosionModifier + RIVER_LEVEL;
        else if (Math.abs(river) < RIVER_THRESHOLD)
            riverModifier = -(continentalModifier + erosionModifier + height * 0.85 - RIVER_LEVEL) * (1 - Utils.smoothInOutQuad(Math.abs(river), 0.005, RIVER_THRESHOLD));
        return riverModifier;
    }

    public static int getResultingHeight(int totalX, int totalZ) {
        double height = GenerationData.heightMapValue(totalX, totalZ);
        double erosion = GenerationData.erosionMapValue(totalX, totalZ);
        double continental = GenerationData.continentalMapValue(totalX, totalZ);
        double river = GenerationData.riverMapValue(totalX, totalZ);
        double ridge = GenerationData.ridgeMapValue(totalX, totalZ);

        return getResultingHeight(height, erosion, continental, river, ridge);
    }

    public static int[][] getResultingHeightMap(double[][] heightMap, double[][] erosionMap, double[][] continentalMap, double[][] riverMap, double[][] ridgeMap) {
        int[][] resultingHeightMap = new int[heightMap.length][heightMap.length];
        for (int mapX = 0; mapX < heightMap.length; mapX++)
            for (int mapZ = 0; mapZ < heightMap.length; mapZ++) {

                double height = heightMap[mapX][mapZ];
                double erosion = erosionMap[mapX][mapZ];
                double continental = continentalMap[mapX][mapZ];
                double river = riverMap[mapX][mapZ];
                double ridge = ridgeMap[mapX][mapZ];

                resultingHeightMap[mapX][mapZ] = getResultingHeight(height, erosion, continental, river, ridge);
            }

        return resultingHeightMap;
    }


    private static int getBiome(GenerationData data) {
        int beachHeight = WATER_LEVEL + (int) (data.feature * 64.0) + 64;
        double dither = data.feature * 0.005f - 0.0025f;
        double temperature = data.temperature + dither;
        double humidity = data.humidity + dither;
        double erosion = data.erosion + dither;
        double continental = data.continental + dither;

        if (data.height < WATER_LEVEL) {
            if (temperature > 0.33) return WARM_OCEAN;
            else if (temperature < -0.33) return COLD_OCEAN;
            return OCEAN;
        }
        if (data.height < beachHeight) {
            return BEACH;
        }
        if (continental > MOUNTAIN_THRESHOLD && erosion < 0.425) {
            if (temperature > 0.33) return DRY_MOUNTAIN;
            else if (temperature < -0.33) return SNOWY_MOUNTAIN;
            return MOUNTAIN;
        }

        if (temperature > 0.33) {
            if (temperature > 0.45 && humidity < -0.3) return CORRODED_MESA;
            if (temperature > 0.55 && humidity < 0.15) return MESA;
            if (humidity < 0.15) return DESERT;
            if (humidity > 0.5 && temperature > 0.5) return BLACK_WOOD_FOREST;
            if (humidity > 0.4 && temperature > 0.4) return DARK_OAK_FOREST;
            return WASTELAND;
        }
        if (humidity > 0.33) {
            if (temperature > -0.1) return REDWOOD_FOREST;
            if (temperature > -0.4) return SPRUCE_FOREST;
            return SNOWY_SPRUCE_FOREST;
        }
        if (humidity < 0.0 && temperature > -0.25) return PLAINS;
        if (humidity > -0.33 && temperature > -0.33) return OAK_FOREST;
        if (humidity < -0.33 && temperature > -0.5) return PINE_FOREST;
        return SNOWY_PLAINS;

    }

    private static final int OCEAN_FLOOR_LEVEL = WATER_LEVEL - 480;
    private static final int DEEP_OCEAN_FLOOR_OFFSET = -1120;
    private static final int FLATLAND_LEVEL = 30 + 15;
    private static final int RIVER_LEVEL = -200;

    private static final double MAX_TERRAIN_HEIGHT_DIFFERENCE = 250;

    private static final double MOUNTAIN_THRESHOLD = 0.3;    // Continental
    private static final double OCEAN_THRESHOLD = -0.3;      // Continental
    private static final double FLATLAND_THRESHOLD = 0.3;    // Erosion
    private static final double RIVER_THRESHOLD = 0.1;       // Erosion

    private static final int DESERT = 0;
    private static final int WASTELAND = 1;
    private static final int DARK_OAK_FOREST = 2;
    private static final int SNOWY_SPRUCE_FOREST = 3;
    private static final int SNOWY_PLAINS = 4;
    private static final int SPRUCE_FOREST = 5;
    private static final int PLAINS = 6;
    private static final int OAK_FOREST = 7;
    private static final int WARM_OCEAN = 8;
    private static final int COLD_OCEAN = 9;
    private static final int OCEAN = 10;
    private static final int DRY_MOUNTAIN = 11;
    private static final int SNOWY_MOUNTAIN = 12;
    private static final int MOUNTAIN = 13;
    private static final int MESA = 14;
    private static final int CORRODED_MESA = 15;
    private static final int BEACH = 16;
    private static final int PINE_FOREST = 17;
    private static final int REDWOOD_FOREST = 18;
    private static final int BLACK_WOOD_FOREST = 19;

    private static final Biome[] BIOMES = new Biome[20];

    private WorldGeneration() {
    }
}