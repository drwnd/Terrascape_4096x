package terrascape.utils;

/**
 * <code>ArrayList</code> is nice and all but the performance sucks compared to this.
 */
public final class ByteArrayList {

    public ByteArrayList(int initialCapacity) {
        data = new byte[Math.max(1, initialCapacity)];
    }

    public void add(byte value) {
        if (size == data.length) grow();
        data[size] = value;
        size++;
    }

    public void copyInto(byte[] target, int startIndex) {
        System.arraycopy(data, 0, target, startIndex, size);
    }

    public int size() {
        return size;
    }

    public void set(byte value, int index) {
        data[index] = value;
    }

    public void pad(int length) {
        size += length;
        if (size >= data.length) grow();
    }


    private void grow() {
        byte[] newData = new byte[Math.max(data.length << 1, size)];
        System.arraycopy(data, 0, newData, 0, data.length);
        data = newData;
    }

    private byte[] data;
    private int size = 0;
}
