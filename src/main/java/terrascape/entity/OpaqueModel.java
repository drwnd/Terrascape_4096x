package terrascape.entity;

import org.joml.Vector3i;

import static terrascape.utils.Constants.*;

public final class OpaqueModel {

    public static final int FACE_TYPE_COUNT = 6;

    public final int X, Y, Z;
    public final int verticesBuffer;
    public final int LOD;
    public final boolean containGeometry;

    public OpaqueModel(Vector3i position, int[] vertexCounts, int verticesBuffer, int lod) {
        containGeometry = vertexCounts != null;
        this.verticesBuffer = verticesBuffer;
        X = position.x;
        Y = position.y;
        Z = position.z;
        LOD = lod;
        if (vertexCounts == null) {
            toRenderVertexCounts = null;
            indices = null;
            this.vertexCounts = null;
            return;
        }
        this.vertexCounts = vertexCounts;
        toRenderVertexCounts = new int[FACE_TYPE_COUNT];
        indices = new int[FACE_TYPE_COUNT];
        indices[0] = 0;
        for (int index = 1; index < FACE_TYPE_COUNT; index++) {
            indices[index] = indices[index - 1] + vertexCounts[index - 1];
        }
    }

    public int[] getVertexCounts(int playerChunkX, int playerChunkY, int playerChunkZ) {
        assert toRenderVertexCounts != null && vertexCounts != null;

        int modelChunkX = X >> CHUNK_SIZE_BITS + LOD;
        int modelChunkY = Y >> CHUNK_SIZE_BITS + LOD;
        int modelChunkZ = Z >> CHUNK_SIZE_BITS + LOD;
        playerChunkX >>= LOD;
        playerChunkY >>= LOD;
        playerChunkZ >>= LOD;

        toRenderVertexCounts[WEST] = playerChunkX >= modelChunkX ? vertexCounts[WEST] : 0;
        toRenderVertexCounts[EAST] = playerChunkX <= modelChunkX ? vertexCounts[EAST] : 0;
        toRenderVertexCounts[TOP] = playerChunkY >= modelChunkY ? vertexCounts[TOP] : 0;
        toRenderVertexCounts[BOTTOM] = playerChunkY <= modelChunkY ? vertexCounts[BOTTOM] : 0;
        toRenderVertexCounts[NORTH] = playerChunkZ >= modelChunkZ ? vertexCounts[NORTH] : 0;
        toRenderVertexCounts[SOUTH] = playerChunkZ <= modelChunkZ ? vertexCounts[SOUTH] : 0;
        return toRenderVertexCounts;
    }

    public int[] getIndices() {
        return indices;
    }

    private final int[] vertexCounts, toRenderVertexCounts;
    private final int[] indices;
}
