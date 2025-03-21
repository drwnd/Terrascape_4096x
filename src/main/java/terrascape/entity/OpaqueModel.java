package terrascape.entity;

import org.joml.Vector3i;

import static terrascape.utils.Constants.*;

public final class OpaqueModel {

    public static final int FACE_TYPE_COUNT = 6;

    public final int X, Y, Z;
    public final int verticesBuffer;

    public OpaqueModel(Vector3i position, int[] vertexCounts, int verticesBuffer) {
        this.verticesBuffer = verticesBuffer;
        this.vertexCounts = vertexCounts;
        X = position.x;
        Y = position.y;
        Z = position.z;
        toRenderVertexCounts = new int[FACE_TYPE_COUNT];
        indices = new int[FACE_TYPE_COUNT];
        indices[0] = 0;
        for (int index = 1; index < FACE_TYPE_COUNT; index++) {
            indices[index] = indices[index - 1] + vertexCounts[index - 1];
        }
    }

    public int[] getVertexCounts(int playerChunkX, int playerChunkY, int playerChunkZ) {
        int modelChunkX = X >> CHUNK_SIZE_BITS;
        int modelChunkY = Y >> CHUNK_SIZE_BITS;
        int modelChunkZ = Z >> CHUNK_SIZE_BITS;

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
