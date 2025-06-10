package terrascape.server;

import terrascape.entity.Light;
import terrascape.utils.ByteArrayList;

import java.awt.*;
import java.util.ArrayList;

import static terrascape.utils.Constants.*;

public final class MaterialsData {

    public MaterialsData(byte material) {
        data = new byte[]{HOMOGENOUS, material};
    }

    private MaterialsData(byte[] data) {
        this.data = data;
    }


    public static MaterialsData loadFromDiscBytes(byte[] bytes, int startIndex) {
        ByteArrayList data = new ByteArrayList(bytes.length);
        getFromDiscBytes(data, bytes, startIndex);
        byte[] dataArray = new byte[data.size()];
        data.copyInto(dataArray, 0);
        return new MaterialsData(dataArray);
    }

    public static MaterialsData getCompressedMaterials(byte[] uncompressedMaterials) {
        ByteArrayList data = new ByteArrayList(1000);
        compressMaterials(data, uncompressedMaterials, CHUNK_SIZE_BITS - 1, 0, 0, 0, 0);
        byte[] dataArray = new byte[data.size()];
        data.copyInto(dataArray, 0);
        return new MaterialsData(dataArray);
    }

    public static int getUncompressedIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY;
    }


    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        synchronized (this) {
            int index = 0, depth = CHUNK_SIZE_BITS - 1;
            while (true) { // Scary but should be fine
                byte identifier = data[index];

                if (identifier == HOMOGENOUS) return data[index + 1];
                if (identifier == DETAIL) return data[index + getInDetailIndex(inChunkX, inChunkY, inChunkZ)];
//            if (identifier == SPLITTER)
                index += getOffset(index, inChunkX, inChunkY, inChunkZ, depth);
                depth--;
            }
        }
    }

    public void fillUncompressedMaterialsInto(byte[] array) {
        synchronized (this) {
            fillUncompressedMaterials(array, CHUNK_SIZE_BITS - 1, 0, 0, 0, 0);
        }
    }

    public void storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size) {
        byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
        fillUncompressedMaterialsInto(uncompressedMaterials);

        size = 1 << size;
        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++)
                for (int y = 0; y < size; y++)
                    uncompressedMaterials[getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z)] = material;

        compressIntoData(uncompressedMaterials);
    }

    public void storeLowerLODChunks(Chunk chunk0, Chunk chunk1, Chunk chunk2, Chunk chunk3,
                                    Chunk chunk4, Chunk chunk5, Chunk chunk6, Chunk chunk7) {

        byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
        fillUncompressedMaterialsInto(uncompressedMaterials);

        storeLowerLODChunk(chunk0, uncompressedMaterials, 0, 0, 0);
        storeLowerLODChunk(chunk1, uncompressedMaterials, 0, 0, CHUNK_SIZE / 2);
        storeLowerLODChunk(chunk2, uncompressedMaterials, 0, CHUNK_SIZE / 2, 0);
        storeLowerLODChunk(chunk3, uncompressedMaterials, 0, CHUNK_SIZE / 2, CHUNK_SIZE / 2);
        storeLowerLODChunk(chunk4, uncompressedMaterials, CHUNK_SIZE / 2, 0, 0);
        storeLowerLODChunk(chunk5, uncompressedMaterials, CHUNK_SIZE / 2, 0, CHUNK_SIZE / 2);
        storeLowerLODChunk(chunk6, uncompressedMaterials, CHUNK_SIZE / 2, CHUNK_SIZE / 2, 0);
        storeLowerLODChunk(chunk7, uncompressedMaterials, CHUNK_SIZE / 2, CHUNK_SIZE / 2, CHUNK_SIZE / 2);

        compressIntoData(uncompressedMaterials);
    }

    public ArrayList<Light> getLights(int lod) {
        ArrayList<Light> lights = new ArrayList<>(0);
        synchronized (this) {
            getLights(lights, lod, CHUNK_SIZE_BITS - 1, 0, 0, 0, 0);
        }
        return lights;
    }

    public int getRAMByteSize() {
        return data.length;
    }

    public byte[] getBytesDiscFormat() {
        ByteArrayList discDataList = new ByteArrayList(data.length);
        synchronized (this) {
            fillDiscData(discDataList, 0);
        }

        byte[] discData = new byte[discDataList.size()];
        discDataList.copyInto(discData, 0);

        return discData;
    }


    private void compressIntoData(byte[] uncompressedMaterials) {
        ByteArrayList dataList = new ByteArrayList(1000);
        compressMaterials(dataList, uncompressedMaterials, CHUNK_SIZE_BITS - 1, 0, 0, 0, 0);

        byte[] dataArray = new byte[dataList.size()];
        dataList.copyInto(dataArray, 0);
        synchronized (this) {
            data = dataArray;
        }
    }

    private void fillDiscData(ByteArrayList discData, int startIndex) {
        byte identifier = data[startIndex];
        discData.add(identifier);

        if (identifier == HOMOGENOUS) {
            discData.add(data[startIndex + 1]);
            return;
        }
        if (identifier == DETAIL) {
            // Weird shuffle for backwards compatibility
            for (int x = 0; x < 4; x++)
                for (int y = 0; y < 4; y++)
                    for (int z = 0; z < 4; z++)
                        discData.add(data[startIndex + getInDetailIndex(x, y, z)]);
            return;
        }
//        if (identifier == SPLITTER)
        fillDiscData(discData, startIndex + SPLITTER_BYTE_SIZE);
        fillDiscData(discData, startIndex + getOffset(startIndex + 1));
        fillDiscData(discData, startIndex + getOffset(startIndex + 4));
        fillDiscData(discData, startIndex + getOffset(startIndex + 7));
        fillDiscData(discData, startIndex + getOffset(startIndex + 10));
        fillDiscData(discData, startIndex + getOffset(startIndex + 13));
        fillDiscData(discData, startIndex + getOffset(startIndex + 16));
        fillDiscData(discData, startIndex + getOffset(startIndex + 19));
    }

    private void storeLowerLODChunk(Chunk chunk, byte[] uncompressedMaterials, int startX, int startY, int startZ) {
        if (chunk == null) return;
        for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX += 2)
            for (int inChunkY = 0; inChunkY < CHUNK_SIZE; inChunkY += 2)
                for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ += 2) {
                    byte material = chunk.getSaveMaterial(inChunkX, inChunkY, inChunkZ);

                    int thisChunkInChunkX = (inChunkX >> 1) + startX;
                    int thisChunkInChunkY = (inChunkY >> 1) + startY;
                    int thisChunkInChunkZ = (inChunkZ >> 1) + startZ;
                    uncompressedMaterials[getUncompressedIndex(thisChunkInChunkX, thisChunkInChunkY, thisChunkInChunkZ)] = material;
                }
    }

    private void fillUncompressedMaterials(byte[] uncompressedMaterials, int depth, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            int size = 1 << depth + 1;
            byte material = data[startIndex + 1];
            for (int x = 0; x < size; x++)
                for (int z = 0; z < size; z++)
                    for (int y = 0; y < size; y++)
                        uncompressedMaterials[getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z)] = material;
            return;
        }
        if (identifier == DETAIL) {
            for (int x = 0; x < 4; x++)
                for (int z = 0; z < 4; z++)
                    for (int y = 0; y < 4; y++) {
                        byte material = data[startIndex + getInDetailIndex(x, y, z)];
                        uncompressedMaterials[getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z)] = material;
                    }
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << depth;
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
    }

    private void getLights(ArrayList<Light> lights, int lod, int depth, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];
        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            int strength = depth + lod + 1 << 24;
            int centerOffset = 1 << depth;
            getLight(inChunkX + centerOffset, inChunkY + centerOffset, inChunkZ + centerOffset, strength, material, lights);
            return;
        }
        if (identifier == DETAIL) {
            get2x2x2LightCube(inChunkX, inChunkY, inChunkZ, lod, lights);
            get2x2x2LightCube(inChunkX, inChunkY, inChunkZ + 2, lod, lights);
            get2x2x2LightCube(inChunkX, inChunkY + 2, inChunkZ, lod, lights);
            get2x2x2LightCube(inChunkX, inChunkY + 2, inChunkZ + 2, lod, lights);
            get2x2x2LightCube(inChunkX + 2, inChunkY, inChunkZ, lod, lights);
            get2x2x2LightCube(inChunkX + 2, inChunkY, inChunkZ + 2, lod, lights);
            get2x2x2LightCube(inChunkX + 2, inChunkY + 2, inChunkZ, lod, lights);
            get2x2x2LightCube(inChunkX + 2, inChunkY + 2, inChunkZ + 2, lod, lights);
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << depth;
        getLights(lights, lod, depth - 1, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
        getLights(lights, lod, depth - 1, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
        getLights(lights, lod, depth - 1, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
        getLights(lights, lod, depth - 1, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        getLights(lights, lod, depth - 1, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
        getLights(lights, lod, depth - 1, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        getLights(lights, lod, depth - 1, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        getLights(lights, lod, depth - 1, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
    }

    private void get2x2x2LightCube(int inChunkX, int inChunkY, int inChunkZ, int lod, ArrayList<Light> lights) {
        byte material0 = getMaterial(inChunkX, inChunkY, inChunkZ);
        byte material1 = getMaterial(inChunkX, inChunkY, inChunkZ + 1);
        byte material2 = getMaterial(inChunkX, inChunkY + 1, inChunkZ);
        byte material3 = getMaterial(inChunkX, inChunkY + 1, inChunkZ + 1);
        byte material4 = getMaterial(inChunkX + 1, inChunkY, inChunkZ);
        byte material5 = getMaterial(inChunkX + 1, inChunkY, inChunkZ + 1);
        byte material6 = getMaterial(inChunkX + 1, inChunkY + 1, inChunkZ);
        byte material7 = getMaterial(inChunkX + 1, inChunkY + 1, inChunkZ + 1);

        if (material0 == material1 && material0 == material2 && material0 == material3 && material0 == material4
                && material0 == material5 && material0 == material6 && material0 == material7) {
            getLight(inChunkX + 1, inChunkY + 1, inChunkZ + 1, lod + 1 << 24, material0, lights);
            return;
        }

        int strength = lod << 24;
        getLight(inChunkX, inChunkY, inChunkZ, strength, material0, lights);
        getLight(inChunkX, inChunkY, inChunkZ + 1, strength, material1, lights);
        getLight(inChunkX, inChunkY + 1, inChunkZ, strength, material2, lights);
        getLight(inChunkX, inChunkY + 1, inChunkZ + 1, strength, material3, lights);
        getLight(inChunkX + 1, inChunkY, inChunkZ, strength, material4, lights);
        getLight(inChunkX + 1, inChunkY, inChunkZ + 1, strength, material5, lights);
        getLight(inChunkX + 1, inChunkY + 1, inChunkZ, strength, material6, lights);
        getLight(inChunkX + 1, inChunkY + 1, inChunkZ + 1, strength, material7, lights);
    }

    private void getLight(int inChunkX, int inChunkY, int inChunkZ, int strength, byte material, ArrayList<Light> lights) {
        if ((Material.getMaterialProperties(material) & EMITS_LIGHT) == 0) return;
        Color light = Material.getMaterialLight(material);
        lights.add(new Light(strength | inChunkX << CHUNK_SIZE_BITS * 2 | inChunkY << CHUNK_SIZE_BITS | inChunkZ, light.getRGB()));
    }

    private int getOffset(int index) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 | data[index + 2] & 0xFF;
    }

    private int getOffset(int splitterIndex, int inChunkX, int inChunkY, int inChunkZ, int depth) {
        int inSplitterIndex = getInSplitterIndex(inChunkX, inChunkY, inChunkZ, depth);
        if (inSplitterIndex == 0) return SPLITTER_BYTE_SIZE;
        return getOffset(splitterIndex + inSplitterIndex - 2);
    }


    private static void setOffset(ByteArrayList data, int offset, int index) {
        data.set((byte) (offset >> 16 & 0xFF), index);
        data.set((byte) (offset >> 8 & 0xFF), index + 1);
        data.set((byte) (offset & 0xFF), index + 2);
    }

    private static int getInDetailIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return ((inChunkX & 3) << 4 | (inChunkZ & 3) << 2 | (inChunkY & 3)) + 1;
    }

    private static int getInSplitterIndex(int inChunkX, int inChunkY, int inChunkZ, int depth) {
        return 3 * ((inChunkX >> depth & 1) << 2 | (inChunkY >> depth & 1) << 1 | (inChunkZ >> depth & 1));
    }

    private static int getFromDiscBytes(ByteArrayList data, byte[] discBytes, int startIndex) {
        byte identifier = discBytes[startIndex];
        data.add(identifier);

        if (identifier == HOMOGENOUS) {
            data.add(discBytes[startIndex + 1]);
            return HOMOGENOUS_BYTE_SIZE;
        }
        if (identifier == DETAIL) {
            // Weird shuffle for backwards compatibility
            for (int x = 0; x < 4; x++)
                for (int y = 0; y < 4; y++)
                    for (int z = 0; z < 4; z++)
                        data.add(discBytes[startIndex + getInDetailIndex(x, y, z)]);
            return DETAIL_BYTE_SIZE;
        }
//        if (identifier == SPLITTER)
        int currentIndex = data.size();
        data.pad(SPLITTER_BYTE_SIZE - 1);
        int discByteSize = 1;

        discByteSize += getFromDiscBytes(data, discBytes, startIndex + discByteSize);
        setOffset(data, data.size() - currentIndex + 1, currentIndex);
        discByteSize += getFromDiscBytes(data, discBytes, startIndex + discByteSize);
        setOffset(data, data.size() - currentIndex + 1, currentIndex + 3);
        discByteSize += getFromDiscBytes(data, discBytes, startIndex + discByteSize);
        setOffset(data, data.size() - currentIndex + 1, currentIndex + 6);
        discByteSize += getFromDiscBytes(data, discBytes, startIndex + discByteSize);
        setOffset(data, data.size() - currentIndex + 1, currentIndex + 9);
        discByteSize += getFromDiscBytes(data, discBytes, startIndex + discByteSize);
        setOffset(data, data.size() - currentIndex + 1, currentIndex + 12);
        discByteSize += getFromDiscBytes(data, discBytes, startIndex + discByteSize);
        setOffset(data, data.size() - currentIndex + 1, currentIndex + 15);
        discByteSize += getFromDiscBytes(data, discBytes, startIndex + discByteSize);
        setOffset(data, data.size() - currentIndex + 1, currentIndex + 18);
        discByteSize += getFromDiscBytes(data, discBytes, startIndex + discByteSize);

        return discByteSize;
    }

    private static int compressMaterials(ByteArrayList data, byte[] uncompressedMaterials, int depth, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        if (isHomogenous(inChunkX, inChunkY, inChunkZ, depth, uncompressedMaterials)) {
            data.add(HOMOGENOUS);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)]);
            return HOMOGENOUS_BYTE_SIZE;
        }
        if (depth < 2) {
            data.add(DETAIL);
            for (int inDetailX = 0; inDetailX < 4; inDetailX++)
                for (int inDetailZ = 0; inDetailZ < 4; inDetailZ++)
                    for (int inDetailY = 0; inDetailY < 4; inDetailY++)
                        data.add(uncompressedMaterials[getUncompressedIndex(inChunkX + inDetailX, inChunkY + inDetailY, inChunkZ + inDetailZ)]);
            return DETAIL_BYTE_SIZE;
        }
        int nextSize = 1 << depth;
        data.add(SPLITTER);
        data.pad(SPLITTER_BYTE_SIZE - 1);
        int offset = SPLITTER_BYTE_SIZE;

        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 1);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 4);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 7);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 10);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 13);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 16);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 19);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);

        return offset;
    }

    private static boolean isHomogenous(final int startX, final int startY, final int startZ, int depth, byte[] uncompressedMaterials) {
        final int size = 1 << depth + 1;
        byte material = uncompressedMaterials[getUncompressedIndex(startX, startY, startZ)];
        for (int inChunkX = startX; inChunkX < startX + size; inChunkX++)
            for (int inChunkZ = startZ; inChunkZ < startZ + size; inChunkZ++)
                for (int inChunkY = startY; inChunkY < startY + size; inChunkY++)
                    if (uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)] != material) return false;
        return true;
    }

    private static final byte HOMOGENOUS = 0;
    private static final byte DETAIL = 1;
    private static final byte SPLITTER = 2;

    private static final byte HOMOGENOUS_BYTE_SIZE = 2;
    private static final byte DETAIL_BYTE_SIZE = 65;
    private static final byte SPLITTER_BYTE_SIZE = 22;

    private byte[] data;
}
