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
uniform ivec3 worldPos;
uniform vec3 cameraPosition;

const vec3[6] normals = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const vec3[6] facePositions = vec3[6](vec3(0, 0, 0), vec3(0, 0, 1), vec3(1, 0, 0), vec3(1, 0, 1), vec3(1, 0, 0), vec3(0, 0, 1));

vec3 getFacePositions(int side, int currentVertexId) {
    vec3 currentVertexOffset = facePositions[currentVertexId].xyz;

    switch (side) {
        case 0: return currentVertexOffset.zxy + vec3(0, 0, 1);
        case 1: return currentVertexOffset.xyz + vec3(0, 1, 0);
        case 2: return currentVertexOffset.yzx + vec3(1, 0, 0);
        case 3: return currentVertexOffset.xzy;
        case 4: return currentVertexOffset.zyx;
        case 5: return currentVertexOffset.yxz;
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

    totalPosition = vec3(x, y, z) + worldPos + getFacePositions(side, currentVertexId);

    gl_Position = projectionViewMatrix * vec4(totalPosition, 1.0);

    material = currentVertex.a >> 21 & 0x7FF;

    //    blockLight = (data.y >> 18 & 15) * 0.0625;
    //    skyLight = (data.y >> 22 & 15) * 0.0625;
    //    ambientOcclusionLevel = 1 - (data.x >> 30 & 3) * 0.22;

    ambientOcclusionLevel = 1;
    blockLight = 0;
    skyLight = 15 * 0.0625;
    normal = normals[side];
}