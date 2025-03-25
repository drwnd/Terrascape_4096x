package terrascape.entity;

import org.joml.Vector3i;

public final class WaterModel {
    public final int X, Y, Z;
    public final int verticesBuffer;
    public final int vertexCount;
    public final int LOD;

    public WaterModel(Vector3i position, int vertexCount, int verticesBuffer, int lod) {
        this.vertexCount = vertexCount;
        this.verticesBuffer = verticesBuffer;
        X = position.x;
        Y = position.y;
        Z = position.z;
        LOD = lod;
    }
}

