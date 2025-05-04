package terrascape.utils;

/**
 * <code> ArrayList<></code> is nice and all but the performance sucks compared to this.
 */
public final class IntArrayList {

    public IntArrayList(int initialCapacity) {
        data = new int[Math.max(1, initialCapacity)];
    }

    public void add(int value) {
        if (size == data.length - 1) grow();
        data[size] = value;
        size++;
    }

    public void clear() {
        size = 0;
    }

    public void copyInto(int[] target, int startIndex) {
        System.arraycopy(data, 0, target, startIndex, size);
    }

    public int size() {
        return size;
    }


    private void grow() {
        int[] newData = new int[data.length << 1];
        System.arraycopy(data, 0, newData, 0, size);
        data = newData;
    }

    private int[] data;
    private int size = 0;
}
