package terrascape.generation;

import terrascape.dataStorage.octree.*;
import terrascape.generation.biomes.Biome;
import terrascape.utils.Utils;

import java.util.Arrays;
import java.util.Random;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.SEED;

public final class GenerationData {

    public double temperature;
    public double humidity;
    public double feature;
    public double erosion;
    public double continental;

    public int height, specialHeight;
    public byte steepness;
    public boolean treeAllowed;

    public Chunk chunk;

    public final int LOD;

    public GenerationData(int chunkX, int chunkZ, int lod) {
        this.LOD = lod;

        temperatureMap = temperatureMap(chunkX, chunkZ, lod);
        humidityMap = humidityMap(chunkX, chunkZ, lod);
        featureMap = featureMap(chunkX, chunkZ, lod);
        treeBitMap = treeBitMap(chunkX, chunkZ, lod);

        erosionMap = erosionMapPadded(chunkX, chunkZ, lod);
        continentalMap = continentalMapPadded(chunkX, chunkZ, lod);
        double[][] heightMap = heightMapPadded(chunkX, chunkZ, lod);
        double[][] riverMap = riverMapPadded(chunkX, chunkZ, lod);
        double[][] ridgeMap = ridgeMapPadded(chunkX, chunkZ, lod);

        resultingHeightMap = WorldGeneration.getResultingHeightMap(heightMap, erosionMap, continentalMap, riverMap, ridgeMap);
        steepnessMap = steepnessMap(resultingHeightMap);
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
        Arrays.fill(uncompressedMaterials, AIR);
        Arrays.fill(cachedMaterials, AIR);
    }

    public void set(int inChunkX, int inChunkZ) {
        int index = inChunkX << CHUNK_SIZE_BITS | inChunkZ;

        temperature = temperatureMap[index];
        humidity = humidityMap[index];
        feature = featureMap[index];
        steepness = steepnessMap[index];
        treeAllowed = (treeBitMap[inChunkX] >> inChunkZ & 1) == 1;

        erosion = erosionMap[inChunkX + 1][inChunkZ + 1];
        continental = continentalMap[inChunkX + 1][inChunkZ + 1];
        height = resultingHeightMap[inChunkX + 1][inChunkZ + 1];
    }

    public void setBiome(int inChunkX, int inChunkZ, Biome biome) {
        specialHeight = biome.getSpecialHeight(getTotalX(inChunkX), getTotalZ(inChunkZ), this);
    }


    public static double heightMapValue(int totalX, int totalZ) {
        double height;
        height = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x08D2BCC9BD98BBF5L, totalX * HEIGHT_MAP_FREQUENCY, totalZ * HEIGHT_MAP_FREQUENCY, 0);
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCEC793764665EF7DL, totalX * HEIGHT_MAP_FREQUENCY * 2, totalZ * HEIGHT_MAP_FREQUENCY * 2, 0) * 0.5;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBD4957D70308DEBFL, totalX * HEIGHT_MAP_FREQUENCY * 4, totalZ * HEIGHT_MAP_FREQUENCY * 4, 0) * 0.25;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xD68F54787A92D53CL, totalX * HEIGHT_MAP_FREQUENCY * 8, totalZ * HEIGHT_MAP_FREQUENCY * 8, 0) * 0.125;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x574730707031DA54L, totalX * HEIGHT_MAP_FREQUENCY * 16, totalZ * HEIGHT_MAP_FREQUENCY * 16, 0) * 0.0625;
        return height;
    }

    public static double temperatureMapValue(int totalX, int totalZ) {
        double temperature;
        temperature = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xADA1CE5C24C4A44FL, totalX * TEMPERATURE_FREQUENCY, totalZ * TEMPERATURE_FREQUENCY, 0) * 0.8888;
        temperature += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEEA0CB5D51C0A447L, totalX * TEMPERATURE_FREQUENCY * 50, totalZ * TEMPERATURE_FREQUENCY * 50, 0) * 0.1111;
        return temperature;
    }

    public static double continentalMapValue(int totalX, int totalZ) {
        double continental;
        continental = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCF71B60E764BFC2CL, totalX * CONTINENTAL_FREQUENCY, totalZ * CONTINENTAL_FREQUENCY, 0) * 0.9588;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x8EF1C1F90DA10C0AL, totalX * CONTINENTAL_FREQUENCY * 6, totalZ * CONTINENTAL_FREQUENCY * 6, 0) * 0.0411;
        return continental;
    }

    public static double riverMapValue(int totalX, int totalZ) {
        double river;
        river = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x84D43603ED399321L, totalX * RIVER_FREQUENCY, totalZ * RIVER_FREQUENCY, 0) * 0.9588;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7C46A6B469AC4A05L, totalX * RIVER_FREQUENCY * 50, totalZ * RIVER_FREQUENCY * 50, 0) * 0.0411;
        return river;
    }

    public static double ridgeMapValue(int totalX, int totalZ) {
        double ridge;
        ridge = (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xDD4D88700A5E4D7EL, totalX * RIDGE_FREQUENCY, totalZ * RIDGE_FREQUENCY, 0))) * 0.5;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x8A3E12DE957E78C5L, totalX * RIDGE_FREQUENCY * 2, totalZ * RIDGE_FREQUENCY * 2, 0))) * 0.25;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x0A8E80B850A75321L, totalX * RIDGE_FREQUENCY * 4, totalZ * RIDGE_FREQUENCY * 4, 0))) * 0.125;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x6E0744EACB517937L, totalX * RIDGE_FREQUENCY * 8, totalZ * RIDGE_FREQUENCY * 8, 0))) * 0.0625;
        return ridge;
    }

    public static double humidityMapValue(int totalX, int totalZ) {
        double humidity;
        humidity = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x41C8F1921D50DF82L, totalX * HUMIDITY_FREQUENCY, totalZ * HUMIDITY_FREQUENCY, 0) * 0.8888;
        humidity += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xB935E00850C8416EL, totalX * HUMIDITY_FREQUENCY * 50, totalZ * HUMIDITY_FREQUENCY * 50, 0) * 0.1111;
        return humidity;
    }

    public static double erosionMapValue(int totalX, int totalZ) {
        double erosion;
        erosion = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBEF86CF6C75F708DL, totalX * EROSION_FREQUENCY, totalZ * EROSION_FREQUENCY, 0) * 0.9588;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x60E4A215EA2087BCL, totalX * EROSION_FREQUENCY * 40, totalZ * EROSION_FREQUENCY * 40, 0) * 0.0411;
        return erosion;
    }


    public int getTotalX(int inChunkX) {
        return chunk.X << CHUNK_SIZE_BITS + LOD | inChunkX * (1 << LOD);
    }

    public int getTotalY(int inChunkY) {
        return chunk.Y << CHUNK_SIZE_BITS + LOD | inChunkY * (1 << LOD);
    }

    public int getTotalZ(int inChunkZ) {
        return chunk.Z << CHUNK_SIZE_BITS + LOD | inChunkZ * (1 << LOD);
    }

    public void store(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        uncompressedMaterials[getIndex(inChunkX, inChunkY, inChunkZ)] = material;
    }

    public ChunkSegment getCompressedMaterials() {
        return getCompressedMaterials(0, 0, 0, (byte) (CHUNK_SIZE_BITS - 1));
    }


    public byte getGeneratingStoneType(int x, int y, int z) {
        // >> 2 for compression and performance improvement
        int compressedX = (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        // Lookup cached value
        int index = compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x1FCA4F81678D9EFEL, x * STONE_TYPE_FREQUENCY, y * STONE_TYPE_FREQUENCY, z * STONE_TYPE_FREQUENCY);
        if (Math.abs(noise) < ANDESITE_THRESHOLD) material = ANDESITE;
        else if (noise > SLATE_THRESHOLD) material = SLATE;
        else if (noise < BLACKSTONE_THRESHOLD) material = BLACKSTONE;
        else material = STONE;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getOceanFloorMaterial(int x, int y, int z) {
        // >> 2 for compression and performance improvement
        int compressedX = (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        // Lookup cached value
        int index = compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x30CD70827706B4C0L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < SAND_THRESHOLD) material = SAND;
        else material = MUD;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getWarmOceanFloorMaterial(int x, int y, int z) {
        // >> 2 for compression and performance improvement
        int compressedX = (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        // Lookup cached value
        int index = compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEB26D0A3459AAA03L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < MUD_THRESHOLD) material = MUD;
        else material = SAND;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getColdOceanFloorMaterial(int x, int y, int z) {
        // >> 2 for compression and performance improvement
        int compressedX = (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        // Lookup cached value
        int index = compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7A182AB93793E000L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < MUD_THRESHOLD) material = MUD;
        else material = GRAVEL;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getGeneratingDirtType(int x, int y, int z) {
        // >> 2 for compression and performance improvement
        int compressedX = (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        // Lookup cached value
        int index = compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF88966EA665D953EL, x * DIRT_TYPE_FREQUENCY, y * DIRT_TYPE_FREQUENCY, z * DIRT_TYPE_FREQUENCY);
        if (Math.abs(noise) < COURSE_DIRT_THRESHOLD) material = COURSE_DIRT;
        else material = DIRT;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getGeneratingIceType(int x, int y, int z) {
        // >> 2 for compression and performance improvement
        int compressedX = (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        // Lookup cached value
        int index = compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xD6744EFC8D01AEFCL, x * ICE_TYPE_FREQUENCY, y * ICE_TYPE_FREQUENCY, z * ICE_TYPE_FREQUENCY);
        if (noise > HEAVY_ICE_THRESHOLD) material = HEAVY_ICE;
        else material = ICE;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getGeneratingGrassType(int x, int y, int z) {
        // >> 2 for compression and performance improvement
        int compressedX = (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        // Lookup cached value
        int index = compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEFB13EFD3B5AC7A7L, x * GRASS_TYPE_FREQUENCY, y * GRASS_TYPE_FREQUENCY, z * GRASS_TYPE_FREQUENCY);
        noise += feature * 0.4 - 0.2;
        if (Math.abs(noise) < MOSS_THRESHOLD) material = MOSS;
        else material = GRASS;

        cachedMaterials[index] = material;
        return material;
    }


    private static double[] temperatureMap(int chunkX, int chunkZ, int lod) {
        double[] temperatureMap = new double[CHUNK_SIZE * CHUNK_SIZE];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++) {
            int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) - gapSize;
            temperatureMap[mapX << CHUNK_SIZE_BITS] = temperatureMapValue(currentX, currentZ);
            temperatureMap[mapX << CHUNK_SIZE_BITS | 1] = temperatureMapValue(currentX, currentZ + gapSize);
        }
        for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
            int currentX = (chunkX << chunkSizeBits) - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
            temperatureMap[mapZ] = temperatureMapValue(currentX, currentZ);
            temperatureMap[1 << CHUNK_SIZE_BITS | mapZ] = temperatureMapValue(currentX + gapSize, currentZ);
        }

        for (int mapX = 3; mapX < CHUNK_SIZE; mapX += 2)
            for (int mapZ = 3; mapZ < CHUNK_SIZE; mapZ += 2) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                double temperature = temperatureMapValue(currentX, currentZ);
                temperatureMap[mapX << CHUNK_SIZE_BITS | mapZ] = temperature;
                double temperatureXMinus2Z = temperatureMap[mapX - 2 << CHUNK_SIZE_BITS | mapZ];
                double temperatureXZMinus2 = temperatureMap[mapX << CHUNK_SIZE_BITS | mapZ - 2];
                temperatureMap[mapX - 1 << CHUNK_SIZE_BITS | mapZ] = 0.5 * (temperature + temperatureXMinus2Z);
                temperatureMap[mapX << CHUNK_SIZE_BITS | mapZ - 1] = 0.5 * (temperature + temperatureXZMinus2);
                temperatureMap[mapX - 1 << CHUNK_SIZE_BITS | mapZ - 1] = 0.25 * (temperature + temperatureXMinus2Z + temperatureXZMinus2 + temperatureMap[mapX - 2 << CHUNK_SIZE_BITS | mapZ - 2]);
            }
        return temperatureMap;
    }

    private static double[] humidityMap(int chunkX, int chunkZ, int lod) {
        double[] humidityMap = new double[CHUNK_SIZE * CHUNK_SIZE];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int currentX = chunkX << chunkSizeBits | mapX * gapSize;
                int currentZ = chunkZ << chunkSizeBits | mapZ * gapSize;
                double humidity = humidityMapValue(currentX, currentZ);
                humidityMap[mapX << CHUNK_SIZE_BITS | mapZ] = humidity;
            }
        return humidityMap;
    }

    private static double[][] heightMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] heightMap = new double[CHUNK_SIZE_PADDED][CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX++) {
            int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) - gapSize;
            heightMap[mapX][0] = heightMapValue(currentX, currentZ);
            heightMap[mapX][1] = heightMapValue(currentX, currentZ + gapSize);
        }
        for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ++) {
            int currentX = (chunkX << chunkSizeBits) - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
            heightMap[0][mapZ] = heightMapValue(currentX, currentZ);
            heightMap[1][mapZ] = heightMapValue(currentX + gapSize, currentZ);
        }

        for (int mapX = 3; mapX < CHUNK_SIZE_PADDED; mapX += 2)
            for (int mapZ = 3; mapZ < CHUNK_SIZE_PADDED; mapZ += 2) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                double height = heightMapValue(currentX, currentZ);
                heightMap[mapX][mapZ] = height;
                heightMap[mapX - 1][mapZ] = 0.5 * (height + heightMap[mapX - 2][mapZ]);
                heightMap[mapX][mapZ - 1] = 0.5 * (height + heightMap[mapX][mapZ - 2]);
                heightMap[mapX - 1][mapZ - 1] = 0.25 * (height + heightMap[mapX - 2][mapZ] + heightMap[mapX][mapZ - 2] + heightMap[mapX - 2][mapZ - 2]);
            }
        return heightMap;
    }

    private static double[][] erosionMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] erosionMap = new double[CHUNK_SIZE_PADDED][CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX++) {
            int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) - gapSize;
            erosionMap[mapX][0] = erosionMapValue(currentX, currentZ);
            erosionMap[mapX][1] = erosionMapValue(currentX, currentZ + gapSize);
        }
        for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ++) {
            int currentX = (chunkX << chunkSizeBits) - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
            erosionMap[0][mapZ] = erosionMapValue(currentX, currentZ);
            erosionMap[1][mapZ] = erosionMapValue(currentX + gapSize, currentZ);
        }

        for (int mapX = 3; mapX < CHUNK_SIZE_PADDED; mapX += 2)
            for (int mapZ = 3; mapZ < CHUNK_SIZE_PADDED; mapZ += 2) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                double erosion = erosionMapValue(currentX, currentZ);
                erosionMap[mapX][mapZ] = erosion;
                erosionMap[mapX - 1][mapZ] = 0.5 * (erosion + erosionMap[mapX - 2][mapZ]);
                erosionMap[mapX][mapZ - 1] = 0.5 * (erosion + erosionMap[mapX][mapZ - 2]);
                erosionMap[mapX - 1][mapZ - 1] = 0.25 * (erosion + erosionMap[mapX - 2][mapZ] + erosionMap[mapX][mapZ - 2] + erosionMap[mapX - 2][mapZ - 2]);
            }
        return erosionMap;
    }

    private static double[] featureMap(int chunkX, int chunkZ, int lod) {
        double[] featureMap = new double[CHUNK_SIZE * CHUNK_SIZE];
        Random random = new Random(Utils.getChunkId(chunkX, lod, chunkZ)); // TODO

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++)
                featureMap[mapX << CHUNK_SIZE_BITS | mapZ] = random.nextDouble();

        return featureMap;
    }

    private static byte[] steepnessMap(int[][] heightMapPadded) {
        byte[] steepnessMap = new byte[CHUNK_SIZE * CHUNK_SIZE];

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int height = heightMapPadded[mapX + 1][mapZ + 1];
                int steepnessX = Math.max(Math.abs(height - heightMapPadded[mapX][mapZ + 1]), Math.abs(height - heightMapPadded[mapX + 2][mapZ + 1]));
                int steepnessZ = Math.max(Math.abs(height - heightMapPadded[mapX + 1][mapZ]), Math.abs(height - heightMapPadded[mapX + 1][mapZ + 2]));
                steepnessMap[mapX << CHUNK_SIZE_BITS | mapZ] = (byte) Math.max(steepnessX, steepnessZ);
            }

        return steepnessMap;
    }

    private static double[][] continentalMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] continentalMap = new double[CHUNK_SIZE_PADDED][CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX++) {
            int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) - gapSize;
            continentalMap[mapX][0] = continentalMapValue(currentX, currentZ);
            continentalMap[mapX][1] = continentalMapValue(currentX, currentZ + gapSize);
        }
        for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ++) {
            int currentX = (chunkX << chunkSizeBits) - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
            continentalMap[0][mapZ] = continentalMapValue(currentX, currentZ);
            continentalMap[1][mapZ] = continentalMapValue(currentX + gapSize, currentZ);
        }

        for (int mapX = 3; mapX < CHUNK_SIZE_PADDED; mapX += 2)
            for (int mapZ = 3; mapZ < CHUNK_SIZE_PADDED; mapZ += 2) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                double continental = continentalMapValue(currentX, currentZ);
                continentalMap[mapX][mapZ] = continental;
                continentalMap[mapX - 1][mapZ] = 0.5 * (continental + continentalMap[mapX - 2][mapZ]);
                continentalMap[mapX][mapZ - 1] = 0.5 * (continental + continentalMap[mapX][mapZ - 2]);
                continentalMap[mapX - 1][mapZ - 1] = 0.25 * (continental + continentalMap[mapX - 2][mapZ] + continentalMap[mapX][mapZ - 2] + continentalMap[mapX - 2][mapZ - 2]);
            }
        return continentalMap;
    }

    private static double[][] riverMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] riverMap = new double[CHUNK_SIZE_PADDED][CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX++) {
            int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) - gapSize;
            riverMap[mapX][0] = riverMapValue(currentX, currentZ);
            riverMap[mapX][1] = riverMapValue(currentX, currentZ + gapSize);
        }
        for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ++) {
            int currentX = (chunkX << chunkSizeBits) - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
            riverMap[0][mapZ] = riverMapValue(currentX, currentZ);
            riverMap[1][mapZ] = riverMapValue(currentX + gapSize, currentZ);
        }

        for (int mapX = 3; mapX < CHUNK_SIZE_PADDED; mapX += 2)
            for (int mapZ = 3; mapZ < CHUNK_SIZE_PADDED; mapZ += 2) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                double river = riverMapValue(currentX, currentZ);
                riverMap[mapX][mapZ] = river;
                riverMap[mapX - 1][mapZ] = 0.5 * (river + riverMap[mapX - 2][mapZ]);
                riverMap[mapX][mapZ - 1] = 0.5 * (river + riverMap[mapX][mapZ - 2]);
                riverMap[mapX - 1][mapZ - 1] = 0.25 * (river + riverMap[mapX - 2][mapZ] + riverMap[mapX][mapZ - 2] + riverMap[mapX - 2][mapZ - 2]);
            }
        return riverMap;
    }

    private static double[][] ridgeMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] ridgeMap = new double[CHUNK_SIZE_PADDED][CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX++) {
            int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) - gapSize;
            ridgeMap[mapX][0] = ridgeMapValue(currentX, currentZ);
            ridgeMap[mapX][1] = ridgeMapValue(currentX, currentZ + gapSize);
        }
        for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ++) {
            int currentX = (chunkX << chunkSizeBits) - gapSize;
            int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
            ridgeMap[0][mapZ] = ridgeMapValue(currentX, currentZ);
            ridgeMap[1][mapZ] = ridgeMapValue(currentX + gapSize, currentZ);
        }

        for (int mapX = 3; mapX < CHUNK_SIZE_PADDED; mapX += 2)
            for (int mapZ = 3; mapZ < CHUNK_SIZE_PADDED; mapZ += 2) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                double ridge = ridgeMapValue(currentX, currentZ);
                ridgeMap[mapX][mapZ] = ridge;
                ridgeMap[mapX - 1][mapZ] = 0.5 * (ridge + ridgeMap[mapX - 2][mapZ]);
                ridgeMap[mapX][mapZ - 1] = 0.5 * (ridge + ridgeMap[mapX][mapZ - 2]);
                ridgeMap[mapX - 1][mapZ - 1] = 0.25 * (ridge + ridgeMap[mapX - 2][mapZ] + ridgeMap[mapX][mapZ - 2] + ridgeMap[mapX - 2][mapZ - 2]);
            }
        return ridgeMap;
    }

    private int[] treeBitMap(int chunkX, int chunkZ, int lod) {
        int[] treeBitMap = new int[CHUNK_SIZE];
        Random random = new Random(Utils.getChunkId(chunkX, lod, chunkZ)); // TODO

        // Places 16 trees each in the central 6 x 6 of the 16 8 x 8s of a chunk
        for (int regionX = 0; regionX < CHUNK_SIZE; regionX += 8)
            for (int regionZ = 0; regionZ < CHUNK_SIZE; regionZ += 8) {
                int inRegionX = random.nextInt(1, 7);
                int inRegionZ = random.nextInt(1, 7);

                treeBitMap[regionX + inRegionX] |= 1 << regionZ + inRegionZ;
            }

        return treeBitMap;
    }


    private ChunkSegment getCompressedMaterials(int x, int y, int z, byte depth) {
        if (isHomogenous(x, y, z, depth)) return new HomogenousSegment(uncompressedMaterials[getIndex(x, y, z)], depth);

        if (depth < 2) {
            DetailSegment segment = new DetailSegment(depth);
            for (int inSegmentX = 0; inSegmentX < 4; inSegmentX++)
                for (int inSegmentY = 0; inSegmentY < 4; inSegmentY++)
                    for (int inSegmentZ = 0; inSegmentZ < 4; inSegmentZ++) {
                        byte material = uncompressedMaterials[getIndex(x + inSegmentX, y + inSegmentY, z + inSegmentZ)];
                        segment.storeNoChecks(inSegmentX, inSegmentY, inSegmentZ, material);
                    }
            return segment;
        }

        int size = 1 << depth;
        ChunkSegment segment0 = getCompressedMaterials(x, y, z, (byte) (depth - 1));
        ChunkSegment segment1 = getCompressedMaterials(x, y, z + size, (byte) (depth - 1));
        ChunkSegment segment2 = getCompressedMaterials(x, y + size, z, (byte) (depth - 1));
        ChunkSegment segment3 = getCompressedMaterials(x, y + size, z + size, (byte) (depth - 1));
        ChunkSegment segment4 = getCompressedMaterials(x + size, y, z, (byte) (depth - 1));
        ChunkSegment segment5 = getCompressedMaterials(x + size, y, z + size, (byte) (depth - 1));
        ChunkSegment segment6 = getCompressedMaterials(x + size, y + size, z, (byte) (depth - 1));
        ChunkSegment segment7 = getCompressedMaterials(x + size, y + size, z + size, (byte) (depth - 1));

        return new SplitterSegment(depth, segment0, segment1, segment2, segment3, segment4, segment5, segment6, segment7);
    }

    private static int getIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY;
    }

    private boolean isHomogenous(int x, int y, int z, byte depth) {
        int size = 1 << depth + 1;
        byte material = uncompressedMaterials[getIndex(x, y, z)];
        for (int inChunkX = x; inChunkX < x + size; inChunkX++)
            for (int inChunkY = y; inChunkY < y + size; inChunkY++)
                for (int inChunkZ = z; inChunkZ < z + size; inChunkZ++)
                    if (uncompressedMaterials[getIndex(inChunkX, inChunkY, inChunkZ)] != material) return false;
        return true;
    }

    private final double[] temperatureMap;
    private final double[] humidityMap;
    private final double[] featureMap;
    private final double[][] erosionMap;
    private final double[][] continentalMap;

    private final int[][] resultingHeightMap;
    private final byte[] steepnessMap;
    private final int[] treeBitMap;
    private final byte[] cachedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE >> 3];

    private final byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

    private static final double TEMPERATURE_FREQUENCY = 6.25E-5;
    private static final double HUMIDITY_FREQUENCY = TEMPERATURE_FREQUENCY;
    private static final double HEIGHT_MAP_FREQUENCY = 1.5625E-4;
    private static final double EROSION_FREQUENCY = 6.25E-5;
    private static final double CONTINENTAL_FREQUENCY = 1.5625E-5;
    private static final double RIVER_FREQUENCY = 3.125E-5;
    private static final double RIDGE_FREQUENCY = 6.125E-5;

    private static final double STONE_TYPE_FREQUENCY = 0.00125;
    private static final double ANDESITE_THRESHOLD = 0.1;
    private static final double SLATE_THRESHOLD = 0.6;
    private static final double BLACKSTONE_THRESHOLD = -0.6;

    private static final double MUD_TYPE_FREQUENCY = 0.0025;
    private static final double GRAVEL_THRESHOLD = 0.1;
    private static final double CLAY_THRESHOLD = 0.5;
    private static final double SAND_THRESHOLD = -0.5;
    private static final double MUD_THRESHOLD = -0.5;

    private static final double DIRT_TYPE_FREQUENCY = 0.003125;
    private static final double COURSE_DIRT_THRESHOLD = 0.15;

    private static final double GRASS_TYPE_FREQUENCY = 0.0015625;
    private static final double MOSS_THRESHOLD = 0.3;

    private static final double ICE_TYPE_FREQUENCY = 0.005;
    private static final double HEAVY_ICE_THRESHOLD = 0.6;

    private static final int CHUNK_SIZE_PADDED = CHUNK_SIZE + 2;
}
