#version 460 core

out float blockLight;
out float skyLight;
out vec3 totalPosition;
out vec3 normal;
flat out int material;

struct particle {
    int x;
    int y;
    int z;
    int material;
    float velocityX;
    float velocityY;
    float velocityZ;
    float gravity;
    float aliveTime;
    float maxLiveTime;
    float roatationSpeedJaw;
    float roatationSpeedPitch;
};

layout (std430, binding = 0) restrict readonly buffer particleBuffer {
    particle[] particles;
};

uniform mat4 projectionViewMatrix;

const vec3[6] normals = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const vec2[6] facePositions = vec2[6](vec2(0, 0), vec2(0, 1), vec2(1, 0), vec2(1, 1), vec2(1, 0), vec2(0, 1));

vec3 getFacePositions(int side, int currentVertexId) {
    vec3 currentVertexOffset = vec3(facePositions[currentVertexId].xy, 0);

    switch (side) {
        case 0: return currentVertexOffset.yxz + vec3(0, 0, 1);
        case 1: return currentVertexOffset.xzy + vec3(0, 1, 0);
        case 2: return currentVertexOffset.zyx + vec3(1, 0, 0);
        case 3: return currentVertexOffset.xyz;
        case 4: return currentVertexOffset.yzx;
        case 5: return currentVertexOffset.zxy;
    }

    return vec3(0, 0, 0);
}

vec3 rotate(vec3 vertexPosition, particle currentParticle) {
    float rotateJaw = currentParticle.roatationSpeedJaw * currentParticle.aliveTime;
    float rotatePitch = currentParticle.roatationSpeedPitch * currentParticle.aliveTime;

    float cosValue = cos(rotateJaw);
    float sinValue = sin(rotateJaw);
    vertexPosition = mat3(1.0, 0.0, 0.0, 0.0, cosValue, -sinValue, 0.0, sinValue, cosValue) * vertexPosition;

    cosValue = cos(rotatePitch);
    sinValue = sin(rotatePitch);
    vertexPosition = mat3(cosValue, 0.0, sinValue, 0.0, 1.0, 0.0, -sinValue, 0.0, cosValue) * vertexPosition;

    return vertexPosition;
}

void main() {

    particle currentParticle = particles[gl_InstanceID];
    int currentVertexId = gl_VertexID % 6;

    float x = float(currentParticle.x);
    float y = float(currentParticle.y);
    float z = float(currentParticle.z);
    int side = (gl_VertexID / 6) % 6;
    float timeScaler = max(0.0, (currentParticle.maxLiveTime - currentParticle.aliveTime) / currentParticle.maxLiveTime);

    vec3 facePosition = getFacePositions(side, currentVertexId);
    vec3 position = vec3(x, y, z) + rotate(facePosition * timeScaler - vec3(timeScaler * 0.5), currentParticle) + vec3(timeScaler * 0.5);
    position += vec3(currentParticle.velocityX, currentParticle.velocityY, currentParticle.velocityZ) * currentParticle.aliveTime;
    position.y -= 0.5 * currentParticle.gravity * currentParticle.aliveTime * currentParticle.aliveTime;

    gl_Position = projectionViewMatrix * vec4(position, 1.0);

    material = side << 8 | currentParticle.material & 0xFF;
    totalPosition = vec3(x, y, z) + facePosition;
    blockLight = 0;
    skyLight = 15 * 0.0625;
    normal = rotate(normals[side], currentParticle);
}