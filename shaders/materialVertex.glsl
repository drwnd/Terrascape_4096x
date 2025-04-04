#version 460 core

in int index;

out float blockLight;
out float skyLight;
out float ambientOcclusionLevel;
out vec3 totalPosition;
out vec3 normal;
flat out int material;

struct vertex {
    int positionData;
    int textureData;
};

layout (std430, binding = 0) restrict readonly buffer vertexBuffer {
    vertex[] vertices;
};

uniform mat4 projectionViewMatrix;
uniform ivec4 worldPos;
uniform vec3 cameraPosition;

const vec3[6] normals = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const vec2[6] facePositions = vec2[6](vec2(0, 0), vec2(0, 1), vec2(1, 0), vec2(1, 1), vec2(1, 0), vec2(0, 1));

vec3 getFacePositions(int side, int currentVertexId, int faceSize1, int faceSize2) {
    vec3 currentVertexOffset = vec3(facePositions[currentVertexId].xy, 0);

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

    vertex currentVertex = vertices[index];
    int currentVertexId = gl_VertexID % 6;

    float x = currentVertex.positionData >> 12 & 63;
    float y = currentVertex.positionData >> 6 & 63;
    float z = currentVertex.positionData & 63;
    int side = currentVertex.textureData >> 8 & 7;

    int faceSize1 = (currentVertex.positionData >> 24 & 63) + 1;
    int faceSize2 = (currentVertex.positionData >> 18 & 63) + 1;
    totalPosition = worldPos.xyz + (vec3(x, y, z) + getFacePositions(side, currentVertexId, faceSize1, faceSize2)) * worldPos.w + vec3(0, -worldPos.w + 1, 0);

    gl_Position = projectionViewMatrix * vec4(totalPosition, 1.0);

    material = currentVertex.textureData;

    ambientOcclusionLevel = 1;
    blockLight = 0;
    skyLight = 15 * 0.0625;
    normal = normals[side];
}