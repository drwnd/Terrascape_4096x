#version 460 core

out vec3 totalPosition;
flat out vec3 normal;
flat out int textureData;

struct Vertex {
    int positionData;
    int textureData;
};

layout (std430, binding = 0) restrict readonly buffer vertexBuffer {
    Vertex[] vertices;
};

uniform mat4 projectionViewMatrix;
uniform ivec4 worldPos;
uniform int indexOffset;
uniform ivec3 iCameraPosition;

const vec3[6] NORMALS = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const vec2[6] FACE_POSITIONS = vec2[6](vec2(0, 0), vec2(0, 1), vec2(1, 0), vec2(1, 1), vec2(1, 0), vec2(0, 1));

vec3 getFacePositions(int side, int currentVertexId, int faceSize1, int faceSize2) {
    vec3 currentVertexOffset = vec3(FACE_POSITIONS[currentVertexId].xy, 0);

    switch (side) {
        case 0: return currentVertexOffset.yxz * vec3(faceSize2, faceSize1, 1) + vec3(0, 0, 1);
        case 1: return currentVertexOffset.xzy * vec3(faceSize1, 1, faceSize2) + vec3(0, 1, 0);
        case 2: return currentVertexOffset.zyx * vec3(1, faceSize1, faceSize2) + vec3(1, 0, 0);
        case 3: return currentVertexOffset.xyz * vec3(faceSize2, faceSize1, 1) + vec3(0, 0, 1);
        case 4: return currentVertexOffset.yzx * vec3(faceSize1, 1, faceSize2) + vec3(0, 1, 0);
        case 5: return currentVertexOffset.zxy * vec3(1, faceSize1, faceSize2) + vec3(1, 0, 0);
    }

    return vec3(0, 0, 0);
}

void main() {
    Vertex currentVertex = vertices[gl_VertexID / 6 + indexOffset];
    int currentVertexId = gl_VertexID % 6;

    float x = currentVertex.positionData >> 12 & 63;
    float y = currentVertex.positionData >> 6 & 63;
    float z = currentVertex.positionData & 63;
    int side = currentVertex.textureData >> 8 & 7;

    int faceSize1 = (currentVertex.positionData >> 24 & 63) + 1;
    int faceSize2 = (currentVertex.positionData >> 18 & 63) + 1;
    totalPosition = (worldPos.xyz - iCameraPosition) + (vec3(x, y, z) + getFacePositions(side, currentVertexId, faceSize1, faceSize2)) * worldPos.w + vec3(0, -worldPos.w + 1, 0);

    gl_Position = projectionViewMatrix * vec4(totalPosition, 1.0);

    textureData = currentVertex.textureData;
    normal = NORMALS[side];
}