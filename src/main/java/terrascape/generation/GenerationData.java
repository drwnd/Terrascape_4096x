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
        specialHeight = biome.getSpecialHeight(chunk.X << CHUNK_SIZE_BITS | inChunkX, chunk.Z << CHUNK_SIZE_BITS | inChunkZ, this);
    }

    private static double[][] heightMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] heightMap = new double[CHUNK_SIZE + 2][CHUNK_SIZE + 2];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE + 2; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE + 2; mapZ++) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
                heightMap[mapX][mapZ] = heightMapValue(currentX, currentZ);
            }
        return heightMap;
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

    private static double[] temperatureMap(int chunkX, int chunkZ, int lod) {
        double[] temperatureMap = new double[CHUNK_SIZE * CHUNK_SIZE];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int currentX = chunkX << chunkSizeBits | mapX * gapSize;
                int currentZ = chunkZ << chunkSizeBits | mapZ * gapSize;
                double temperature = temperatureMapValue(currentX, currentZ);
                temperatureMap[mapX << CHUNK_SIZE_BITS | mapZ] = temperature;
            }
        return temperatureMap;
    }

    public static double temperatureMapValue(int totalX, int totalZ) {
        double temperature;
        temperature = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xADA1CE5C24C4A44FL, totalX * TEMPERATURE_FREQUENCY, totalZ * TEMPERATURE_FREQUENCY, 0) * 0.8888;
        temperature += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEEA0CB5D51C0A447L, totalX * TEMPERATURE_FREQUENCY * 50, totalZ * TEMPERATURE_FREQUENCY * 50, 0) * 0.1111;
        return temperature;
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

    public static double humidityMapValue(int totalX, int totalZ) {
        double humidity;
        humidity = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x41C8F1921D50DF82L, totalX * HUMIDITY_FREQUENCY, totalZ * HUMIDITY_FREQUENCY, 0) * 0.8888;
        humidity += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xB935E00850C8416EL, totalX * HUMIDITY_FREQUENCY * 50, totalZ * HUMIDITY_FREQUENCY * 50, 0) * 0.1111;
        return humidity;
    }

    private static double[][] erosionMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] erosionMap = new double[CHUNK_SIZE + 2][CHUNK_SIZE + 2];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE + 2; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE + 2; mapZ++) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
                double erosion = erosionMapValue(currentX, currentZ);
                erosionMap[mapX][mapZ] = erosion;
            }
        return erosionMap;
    }

    public static double erosionMapValue(int totalX, int totalZ) {
        double erosion;
        erosion = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBEF86CF6C75F708DL, totalX * EROSION_FREQUENCY, totalZ * EROSION_FREQUENCY, 0) * 0.9588;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x60E4A215EA2087BCL, totalX * EROSION_FREQUENCY * 40, totalZ * EROSION_FREQUENCY * 40, 0) * 0.0411;
        return erosion;
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
        double[][] continentalMap = new double[CHUNK_SIZE + 2][CHUNK_SIZE + 2];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE + 2; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE + 2; mapZ++) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
                double continental = continentalMapValue(currentX, currentZ);
                continentalMap[mapX][mapZ] = continental;
            }
        return continentalMap;
    }

    public static double continentalMapValue(int totalX, int totalZ) {
        double continental;
        continental = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCF71B60E764BFC2CL, totalX * CONTINENTAL_FREQUENCY, totalZ * CONTINENTAL_FREQUENCY, 0) * 0.9588;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x8EF1C1F90DA10C0AL, totalX * CONTINENTAL_FREQUENCY * 50, totalZ * CONTINENTAL_FREQUENCY * 50, 0) * 0.0411;
        return continental;
    }

    private static double[][] riverMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] riverMap = new double[CHUNK_SIZE + 2][CHUNK_SIZE + 2];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE + 2; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE + 2; mapZ++) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
                double river = riverMapValue(currentX, currentZ);
                riverMap[mapX][mapZ] = river;
            }
        return riverMap;
    }

    public static double riverMapValue(int totalX, int totalZ) {
        double river;
        river = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x84D43603ED399321L, totalX * RIVER_FREQUENCY, totalZ * RIVER_FREQUENCY, 0) * 0.9588;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7C46A6B469AC4A05L, totalX * RIVER_FREQUENCY * 50, totalZ * RIVER_FREQUENCY * 50, 0) * 0.0411;
        return river;
    }

    private static double[][] ridgeMapPadded(int chunkX, int chunkZ, int lod) {
        double[][] ridgeMap = new double[CHUNK_SIZE + 2][CHUNK_SIZE + 2];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE + 2; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE + 2; mapZ++) {
                int currentX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int currentZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;
                double river = ridgeMapValue(currentX, currentZ);
                ridgeMap[mapX][mapZ] = river;
            }
        return ridgeMap;
    }

    public static double ridgeMapValue(int totalX, int totalZ) {
        double ridge;
        ridge = (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xDD4D88700A5E4D7EL, totalX * RIDGE_FREQUENCY, totalZ * RIDGE_FREQUENCY, 0))) * 0.5;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x8A3E12DE957E78C5L, totalX * RIDGE_FREQUENCY * 2, totalZ * RIDGE_FREQUENCY * 2, 0))) * 0.25;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x0A8E80B850A75321L, totalX * RIDGE_FREQUENCY * 4, totalZ * RIDGE_FREQUENCY * 4, 0))) * 0.125;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x6E0744EACB517937L, totalX * RIDGE_FREQUENCY * 8, totalZ * RIDGE_FREQUENCY * 8, 0))) * 0.0625;
        return ridge;
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

    private final byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

    private static final double TEMPERATURE_FREQUENCY = 6.25E-5;
    private static final double HUMIDITY_FREQUENCY = TEMPERATURE_FREQUENCY;
    private static final double HEIGHT_MAP_FREQUENCY = 1.5625E-4;
    private static final double EROSION_FREQUENCY = 6.25E-5;
    private static final double CONTINENTAL_FREQUENCY = 1.5625E-5;
    private static final double RIVER_FREQUENCY = 3.125E-5;
    private static final double RIDGE_FREQUENCY = 3.125E-4;
}
