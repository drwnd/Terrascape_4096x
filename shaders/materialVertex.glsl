#version 460 core

in int index;

out float blockLight;
out float skyLight;
out float ambientOcclusionLevel;
out vec3 totalPosition;
out vec3 normal;
flat out int material;

struct vertex {
    int a;
    int b;
};

layout (std430, binding = 0) restrict readonly buffer vertexBuffer {
    vertex[] vertices;
};

uniform mat4 projectionViewMatrix;
uniform ivec4 worldPos;
uniform vec3 cameraPosition;

const vec3[6] normals = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const vec3[6] facePositions = vec3[6](vec3(0, 0, 0), vec3(0, 0, 1), vec3(1, 0, 0), vec3(1, 0, 1), vec3(1, 0, 0), vec3(0, 0, 1));

vec3 getFacePositions(int side, int currentVertexId, int faceSize) {
    vec3 currentVertexOffset = facePositions[currentVertexId].xyz;
    int size1 = (faceSize >> 7 & 127) + 1;
    int size2 = (faceSize & 127) + 1;

    switch (side) {
        case 0: return currentVertexOffset.zxy * vec3(size2, size1, 1) + vec3(0, 0, 1);
        case 1: return currentVertexOffset.xyz * vec3(size1, 1, size2) + vec3(0, 1, 0);
        case 2: return currentVertexOffset.yzx * vec3(1, size1, size2) + vec3(1, 0, 0);
        case 3: return currentVertexOffset.xzy * vec3(size2, size1, 1) + vec3(0, 0, 1);
        case 4: return currentVertexOffset.zyx * vec3(size1, 1, size2) + vec3(0, 1, 0);
        case 5: return currentVertexOffset.yxz * vec3(1, size1, size2) + vec3(1, 0, 0);
    }

    return currentVertexOffset;
}

void main() {

    vertex currentVertex = vertices[index];
    int currentVertexId = gl_VertexID % 6;

    float x = (currentVertex.a >> 14 & 127);
    float y = (currentVertex.a >> 7 & 127);
    float z = (currentVertex.a & 127);
    int side = currentVertex.a >> 29 & 7;

    totalPosition = worldPos.xyz + (vec3(x, y, z) + getFacePositions(side, currentVertexId, currentVertex.b)) * worldPos.w + vec3(0, -worldPos.w + 1, 0);

    gl_Position = projectionViewMatrix * vec4(totalPosition, 1.0);

    material = currentVertex.a >> 21 & 0x7FF;

    ambientOcclusionLevel = 1;
    blockLight = 0;
    skyLight = 15 * 0.0625;
    normal = normals[side];
}