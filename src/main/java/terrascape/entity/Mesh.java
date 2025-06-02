package terrascape.entity;

public record Mesh(int[] opaqueVertices, int[] vertexCounts,
                   int[] transparentVertices, int waterVertexCount, int glassVertexCount,
                   int[] lights) {
}
