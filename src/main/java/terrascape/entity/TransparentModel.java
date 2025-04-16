package terrascape.entity;

import org.joml.Vector3i;

public final class TransparentModel {
    public final int X, Y, Z;
    public final int verticesBuffer;
    public final int waterVertexCount, glassVertexCount;
    public final int LOD;
    public final boolean containsGeometry;

    public TransparentModel(Vector3i position, int waterVertexCount, int glassVertexCount, int verticesBuffer, int lod) {
        containsGeometry = waterVertexCount + glassVertexCount != 0;
        this.waterVertexCount = waterVertexCount;
        this.glassVertexCount = glassVertexCount;
        this.verticesBuffer = verticesBuffer;
        X = position.x;
        Y = position.y;
        Z = position.z;
        LOD = lod;
    }
}

