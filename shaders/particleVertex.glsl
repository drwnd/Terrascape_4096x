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
    int packedVelocityGravity;
    int packedLifeTimeRotationMaterial;
    float aliveTime;
};

layout (std430, binding = 0) restrict readonly buffer particleBuffer {
    particle[] particles;
};

uniform mat4 projectionViewMatrix;

const vec3[6] normals = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const vec2[6] facePositions = vec2[6](vec2(0, 0), vec2(0, 1), vec2(1, 0), vec2(1, 1), vec2(1, 0), vec2(0, 1));

const float VELOCITY_PACKING_FACTOR = 0.25; // Inverse in Particle.java
const float GRAVITY_PACKING_FACTOR = 0.5; // Inverse in Particle.java
const float ROTATION_PACKING_FACTOR = 0.0625; // Inverse in Particle.java

float getTimeScaler(particle currentParticle) {
    float maxLiveTime = float(currentParticle.packedLifeTimeRotationMaterial >> 24) / 20.0;

    return max(0.0, (maxLiveTime - currentParticle.aliveTime) / maxLiveTime);;
}

float getGravity(particle currentParticle) {
    return (currentParticle.packedVelocityGravity & 0xFF) * GRAVITY_PACKING_FACTOR;
}

vec2 getRotationSpeed(particle currentParticle) {
    float rotationSpeedX = (currentParticle.packedLifeTimeRotationMaterial >> 16 & 0xFF) * ROTATION_PACKING_FACTOR;
    float rotationSpeedY = (currentParticle.packedLifeTimeRotationMaterial >> 8 & 0xFF) * ROTATION_PACKING_FACTOR;

    return vec2(rotationSpeedX, rotationSpeedY);
}

vec3 getVelocity(particle currentParticle) {
    float velocityX = ((currentParticle.packedVelocityGravity >> 24 & 0xFF) - 128) * VELOCITY_PACKING_FACTOR;
    float velocityY = ((currentParticle.packedVelocityGravity >> 16 & 0xFF) - 128) * VELOCITY_PACKING_FACTOR;
    float velocityZ = ((currentParticle.packedVelocityGravity >> 8 & 0xFF) - 128) * VELOCITY_PACKING_FACTOR;

    return vec3(velocityX, velocityY, velocityZ);
}

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
    vec2 rotation = getRotationSpeed(currentParticle) * currentParticle.aliveTime;

    float cosValue = cos(rotation.x);
    float sinValue = sin(rotation.x);
    vertexPosition = mat3(1.0, 0.0, 0.0, 0.0, cosValue, -sinValue, 0.0, sinValue, cosValue) * vertexPosition;

    cosValue = cos(rotation.y);
    sinValue = sin(rotation.y);
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
    float timeScaler = getTimeScaler(currentParticle);

    vec3 facePosition = getFacePositions(side, currentVertexId);
    vec3 position = vec3(x, y, z) + rotate(facePosition * timeScaler - vec3(timeScaler * 0.5), currentParticle) + vec3(timeScaler * 0.5);
    position += getVelocity(currentParticle) * currentParticle.aliveTime;
    position.y -= 0.5 * getGravity(currentParticle) * currentParticle.aliveTime * currentParticle.aliveTime;

    gl_Position = projectionViewMatrix * vec4(position, 1.0);

    material = side << 8 | currentParticle.packedLifeTimeRotationMaterial & 0xFF;
    totalPosition = vec3(x, y, z) + facePosition;
    blockLight = 0;
    skyLight = 15 * 0.0625;
    normal = rotate(normals[side], currentParticle);
}