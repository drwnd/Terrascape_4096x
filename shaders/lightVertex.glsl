#version 430 core

in vec3 offset;

struct Light {
    int inChunkPosition;
    int color;
};

layout (std430, binding = 0) restrict readonly buffer lightsBuffer {
    Light[] lights;
};

const float RADIUS_MULTIPLIER = 100;

uniform mat4 projectionViewMatrix;
uniform ivec3 iCameraPosition;
uniform ivec4 worldPos;

flat out ivec3 centerPosition;
flat out vec3 color;
flat out float squareRadius;
flat out float strength;

void main() {
    Light currentLight = lights[gl_InstanceID];

    float sideLength = 1 << (currentLight.inChunkPosition >> 24 & 255);
    strength = sideLength * sideLength * sideLength;

    float red = (currentLight.color >> 16 & 255) * 0.00390625;
    float green = (currentLight.color >> 8 & 255) * 0.00390625;
    float blue = (currentLight.color & 255) * 0.00390625;
    color = vec3(red, green, blue);

    int x = currentLight.inChunkPosition >> 12 & 63;
    int y = currentLight.inChunkPosition >> 6 & 63;
    int z = currentLight.inChunkPosition & 63;
    centerPosition = (worldPos.xyz - iCameraPosition) + ivec3(x, y, z) * worldPos.w;
    squareRadius = RADIUS_MULTIPLIER * sideLength * sideLength;

    float radius = sqrt(squareRadius) * 1.1;
    vec3 position = centerPosition + offset * radius;
    gl_Position = projectionViewMatrix * vec4(position, 1);
}
