package terrascape.dataStorage.octree;

import java.util.ArrayList;

import static terrascape.utils.Constants.OUT_OF_WORLD;

public final class DetailSegment extends ChunkSegment {

    DetailSegment() {

    }

    DetailSegment(byte material) {
        material0 = material;
        material1 = material;
        material2 = material;
        material3 = material;
        material4 = material;
        material5 = material;
        material6 = material;
        material7 = material;
        material8 = material;
        material9 = material;
        material10 = material;
        material11 = material;
        material12 = material;
        material13 = material;
        material14 = material;
        material15 = material;
        material16 = material;
        material17 = material;
        material18 = material;
        material19 = material;
        material20 = material;
        material21 = material;
        material22 = material;
        material23 = material;
        material24 = material;
        material25 = material;
        material26 = material;
        material27 = material;
        material28 = material;
        material29 = material;
        material30 = material;
        material31 = material;
        material32 = material;
        material33 = material;
        material34 = material;
        material35 = material;
        material36 = material;
        material37 = material;
        material38 = material;
        material39 = material;
        material40 = material;
        material41 = material;
        material42 = material;
        material43 = material;
        material44 = material;
        material45 = material;
        material46 = material;
        material47 = material;
        material48 = material;
        material49 = material;
        material50 = material;
        material51 = material;
        material52 = material;
        material53 = material;
        material54 = material;
        material55 = material;
        material56 = material;
        material57 = material;
        material58 = material;
        material59 = material;
        material60 = material;
        material61 = material;
        material62 = material;
        material63 = material;
    }

    static DetailSegment parseDetail(byte[] bytes, int startIndex) {
        DetailSegment segment = new DetailSegment();
        segment.material0 = bytes[++startIndex];
        segment.material1 = bytes[++startIndex];
        segment.material2 = bytes[++startIndex];
        segment.material3 = bytes[++startIndex];
        segment.material4 = bytes[++startIndex];
        segment.material5 = bytes[++startIndex];
        segment.material6 = bytes[++startIndex];
        segment.material7 = bytes[++startIndex];
        segment.material8 = bytes[++startIndex];
        segment.material9 = bytes[++startIndex];
        segment.material10 = bytes[++startIndex];
        segment.material11 = bytes[++startIndex];
        segment.material12 = bytes[++startIndex];
        segment.material13 = bytes[++startIndex];
        segment.material14 = bytes[++startIndex];
        segment.material15 = bytes[++startIndex];
        segment.material16 = bytes[++startIndex];
        segment.material17 = bytes[++startIndex];
        segment.material18 = bytes[++startIndex];
        segment.material19 = bytes[++startIndex];
        segment.material20 = bytes[++startIndex];
        segment.material21 = bytes[++startIndex];
        segment.material22 = bytes[++startIndex];
        segment.material23 = bytes[++startIndex];
        segment.material24 = bytes[++startIndex];
        segment.material25 = bytes[++startIndex];
        segment.material26 = bytes[++startIndex];
        segment.material27 = bytes[++startIndex];
        segment.material28 = bytes[++startIndex];
        segment.material29 = bytes[++startIndex];
        segment.material30 = bytes[++startIndex];
        segment.material31 = bytes[++startIndex];
        segment.material32 = bytes[++startIndex];
        segment.material33 = bytes[++startIndex];
        segment.material34 = bytes[++startIndex];
        segment.material35 = bytes[++startIndex];
        segment.material36 = bytes[++startIndex];
        segment.material37 = bytes[++startIndex];
        segment.material38 = bytes[++startIndex];
        segment.material39 = bytes[++startIndex];
        segment.material40 = bytes[++startIndex];
        segment.material41 = bytes[++startIndex];
        segment.material42 = bytes[++startIndex];
        segment.material43 = bytes[++startIndex];
        segment.material44 = bytes[++startIndex];
        segment.material45 = bytes[++startIndex];
        segment.material46 = bytes[++startIndex];
        segment.material47 = bytes[++startIndex];
        segment.material48 = bytes[++startIndex];
        segment.material49 = bytes[++startIndex];
        segment.material50 = bytes[++startIndex];
        segment.material51 = bytes[++startIndex];
        segment.material52 = bytes[++startIndex];
        segment.material53 = bytes[++startIndex];
        segment.material54 = bytes[++startIndex];
        segment.material55 = bytes[++startIndex];
        segment.material56 = bytes[++startIndex];
        segment.material57 = bytes[++startIndex];
        segment.material58 = bytes[++startIndex];
        segment.material59 = bytes[++startIndex];
        segment.material60 = bytes[++startIndex];
        segment.material61 = bytes[++startIndex];
        segment.material62 = bytes[++startIndex];
        segment.material63 = bytes[++startIndex];
        return segment;
    }

    @Override
    byte getMaterial(int inChunkX, int inChunkY, int inChunkZ, int depth) {
        int index = (inChunkX & 3) << 4 | (inChunkY & 3) << 2 | (inChunkZ & 3);
        return switch (index & 63) {
            case 0 -> material0;
            case 1 -> material1;
            case 2 -> material2;
            case 3 -> material3;
            case 4 -> material4;
            case 5 -> material5;
            case 6 -> material6;
            case 7 -> material7;
            case 8 -> material8;
            case 9 -> material9;
            case 10 -> material10;
            case 11 -> material11;
            case 12 -> material12;
            case 13 -> material13;
            case 14 -> material14;
            case 15 -> material15;
            case 16 -> material16;
            case 17 -> material17;
            case 18 -> material18;
            case 19 -> material19;
            case 20 -> material20;
            case 21 -> material21;
            case 22 -> material22;
            case 23 -> material23;
            case 24 -> material24;
            case 25 -> material25;
            case 26 -> material26;
            case 27 -> material27;
            case 28 -> material28;
            case 29 -> material29;
            case 30 -> material30;
            case 31 -> material31;
            case 32 -> material32;
            case 33 -> material33;
            case 34 -> material34;
            case 35 -> material35;
            case 36 -> material36;
            case 37 -> material37;
            case 38 -> material38;
            case 39 -> material39;
            case 40 -> material40;
            case 41 -> material41;
            case 42 -> material42;
            case 43 -> material43;
            case 44 -> material44;
            case 45 -> material45;
            case 46 -> material46;
            case 47 -> material47;
            case 48 -> material48;
            case 49 -> material49;
            case 50 -> material50;
            case 51 -> material51;
            case 52 -> material52;
            case 53 -> material53;
            case 54 -> material54;
            case 55 -> material55;
            case 56 -> material56;
            case 57 -> material57;
            case 58 -> material58;
            case 59 -> material59;
            case 60 -> material60;
            case 61 -> material61;
            case 62 -> material62;
            case 63 -> material63;
            default -> OUT_OF_WORLD; // Mathematically unreachable
        };
    }

    @Override
    ChunkSegment storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int size, int depth) {

        if (size == 0) storeNoChecks(inChunkX, inChunkY, inChunkZ, material);
        if (size == 1) {
            storeNoChecks(inChunkX, inChunkY, inChunkZ, material);
            storeNoChecks(inChunkX, inChunkY, inChunkZ + 1, material);
            storeNoChecks(inChunkX, inChunkY + 1, inChunkZ, material);
            storeNoChecks(inChunkX, inChunkY + 1, inChunkZ + 1, material);
            storeNoChecks(inChunkX + 1, inChunkY, inChunkZ, material);
            storeNoChecks(inChunkX + 1, inChunkY, inChunkZ + 1, material);
            storeNoChecks(inChunkX + 1, inChunkY + 1, inChunkZ, material);
            storeNoChecks(inChunkX + 1, inChunkY + 1, inChunkZ + 1, material);
        }
        if (size == 2) return new HomogenousSegment(material);

        if (material0 == material && material1 == material && material2 == material && material3 == material
                && material4 == material && material5 == material && material6 == material && material7 == material
                && material8 == material && material9 == material && material10 == material && material11 == material
                && material12 == material && material13 == material && material14 == material && material15 == material
                && material16 == material && material17 == material && material18 == material && material19 == material
                && material20 == material && material21 == material && material22 == material && material23 == material
                && material24 == material && material25 == material && material26 == material && material27 == material
                && material28 == material && material29 == material && material30 == material && material31 == material
                && material32 == material && material33 == material && material34 == material && material35 == material
                && material36 == material && material37 == material && material38 == material && material39 == material
                && material40 == material && material41 == material && material42 == material && material43 == material
                && material44 == material && material45 == material && material46 == material && material47 == material
                && material48 == material && material49 == material && material50 == material && material51 == material
                && material52 == material && material53 == material && material54 == material && material55 == material
                && material56 == material && material57 == material && material58 == material && material59 == material
                && material60 == material && material61 == material && material62 == material && material63 == material)
            return new HomogenousSegment(material);
        return this;
    }

    public void storeNoChecks(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        int index = (inChunkX & 3) << 4 | (inChunkY & 3) << 2 | (inChunkZ & 3);
        switch (index & 63) {
            case 0 -> material0 = material;
            case 1 -> material1 = material;
            case 2 -> material2 = material;
            case 3 -> material3 = material;
            case 4 -> material4 = material;
            case 5 -> material5 = material;
            case 6 -> material6 = material;
            case 7 -> material7 = material;
            case 8 -> material8 = material;
            case 9 -> material9 = material;
            case 10 -> material10 = material;
            case 11 -> material11 = material;
            case 12 -> material12 = material;
            case 13 -> material13 = material;
            case 14 -> material14 = material;
            case 15 -> material15 = material;
            case 16 -> material16 = material;
            case 17 -> material17 = material;
            case 18 -> material18 = material;
            case 19 -> material19 = material;
            case 20 -> material20 = material;
            case 21 -> material21 = material;
            case 22 -> material22 = material;
            case 23 -> material23 = material;
            case 24 -> material24 = material;
            case 25 -> material25 = material;
            case 26 -> material26 = material;
            case 27 -> material27 = material;
            case 28 -> material28 = material;
            case 29 -> material29 = material;
            case 30 -> material30 = material;
            case 31 -> material31 = material;
            case 32 -> material32 = material;
            case 33 -> material33 = material;
            case 34 -> material34 = material;
            case 35 -> material35 = material;
            case 36 -> material36 = material;
            case 37 -> material37 = material;
            case 38 -> material38 = material;
            case 39 -> material39 = material;
            case 40 -> material40 = material;
            case 41 -> material41 = material;
            case 42 -> material42 = material;
            case 43 -> material43 = material;
            case 44 -> material44 = material;
            case 45 -> material45 = material;
            case 46 -> material46 = material;
            case 47 -> material47 = material;
            case 48 -> material48 = material;
            case 49 -> material49 = material;
            case 50 -> material50 = material;
            case 51 -> material51 = material;
            case 52 -> material52 = material;
            case 53 -> material53 = material;
            case 54 -> material54 = material;
            case 55 -> material55 = material;
            case 56 -> material56 = material;
            case 57 -> material57 = material;
            case 58 -> material58 = material;
            case 59 -> material59 = material;
            case 60 -> material60 = material;
            case 61 -> material61 = material;
            case 62 -> material62 = material;
            case 63 -> material63 = material;
        }
    }

    @Override
    public int getByteSize() {
        return 65;
    }

    @Override
    public void addBytes(ArrayList<Byte> bytes) {
        bytes.add(DETAIL);
        bytes.add(material0);
        bytes.add(material1);
        bytes.add(material2);
        bytes.add(material3);
        bytes.add(material4);
        bytes.add(material5);
        bytes.add(material6);
        bytes.add(material7);
        bytes.add(material8);
        bytes.add(material9);
        bytes.add(material10);
        bytes.add(material11);
        bytes.add(material12);
        bytes.add(material13);
        bytes.add(material14);
        bytes.add(material15);
        bytes.add(material16);
        bytes.add(material17);
        bytes.add(material18);
        bytes.add(material19);
        bytes.add(material20);
        bytes.add(material21);
        bytes.add(material22);
        bytes.add(material23);
        bytes.add(material24);
        bytes.add(material25);
        bytes.add(material26);
        bytes.add(material27);
        bytes.add(material28);
        bytes.add(material29);
        bytes.add(material30);
        bytes.add(material31);
        bytes.add(material32);
        bytes.add(material33);
        bytes.add(material34);
        bytes.add(material35);
        bytes.add(material36);
        bytes.add(material37);
        bytes.add(material38);
        bytes.add(material39);
        bytes.add(material40);
        bytes.add(material41);
        bytes.add(material42);
        bytes.add(material43);
        bytes.add(material44);
        bytes.add(material45);
        bytes.add(material46);
        bytes.add(material47);
        bytes.add(material48);
        bytes.add(material49);
        bytes.add(material50);
        bytes.add(material51);
        bytes.add(material52);
        bytes.add(material53);
        bytes.add(material54);
        bytes.add(material55);
        bytes.add(material56);
        bytes.add(material57);
        bytes.add(material58);
        bytes.add(material59);
        bytes.add(material60);
        bytes.add(material61);
        bytes.add(material62);
        bytes.add(material63);
    }

    @Override
    public byte getType() {
        return DETAIL;
    }

    private byte material0;
    private byte material1;
    private byte material2;
    private byte material3;
    private byte material4;
    private byte material5;
    private byte material6;
    private byte material7;
    private byte material8;
    private byte material9;
    private byte material10;
    private byte material11;
    private byte material12;
    private byte material13;
    private byte material14;
    private byte material15;
    private byte material16;
    private byte material17;
    private byte material18;
    private byte material19;
    private byte material20;
    private byte material21;
    private byte material22;
    private byte material23;
    private byte material24;
    private byte material25;
    private byte material26;
    private byte material27;
    private byte material28;
    private byte material29;
    private byte material30;
    private byte material31;
    private byte material32;
    private byte material33;
    private byte material34;
    private byte material35;
    private byte material36;
    private byte material37;
    private byte material38;
    private byte material39;
    private byte material40;
    private byte material41;
    private byte material42;
    private byte material43;
    private byte material44;
    private byte material45;
    private byte material46;
    private byte material47;
    private byte material48;
    private byte material49;
    private byte material50;
    private byte material51;
    private byte material52;
    private byte material53;
    private byte material54;
    private byte material55;
    private byte material56;
    private byte material57;
    private byte material58;
    private byte material59;
    private byte material60;
    private byte material61;
    private byte material62;
    private byte material63;
}
