#version 400 core

flat in int textureData;
in vec3 normal;
in vec3 totalPosition;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform int flags;
uniform float time;
uniform vec3 cameraPosition;
uniform vec3 sunDirection;

const int HEAD_UNDER_WATER_BIT = 1;

float easeInOutQuart(float x) {
    //x < 0.5 ? 8 * x * x * x * x : 1 - pow(-2 * x + 2, 4) / 2;
    float inValue = 8.0 * x * x * x * x;
    float outValue = 1.0 - pow(-2.0 * x + 2.0, 4.0) / 2.0;
    return step(inValue, 0.5) * inValue + step(0.5, outValue) * outValue;
}

float getBlockLight() {
    return 0.0;
}

int isFlag(int bit) {
    return int((flags & bit) != 0);
}

float getSkyLight() {
    return 1.0;
}

vec2 getUVOffset(int side) {
    switch (side) {
        case 0: return vec2(fract(totalPosition.x * 0.0625), 1 - fract(totalPosition.y * 0.0625)) * 0.0625;
        case 1: return fract(totalPosition.xz * 0.0625) * 0.0625;
        case 2: return 0.0625 - fract(totalPosition.zy * 0.0625) * 0.0625;
        case 3: return 0.0625 - fract(totalPosition.xy * 0.0625) * 0.0625;
        case 4: return fract(totalPosition.zx * 0.0625) * 0.0625;
        case 5: return vec2(fract(totalPosition.z * 0.0625), 1 - fract(totalPosition.y * 0.0625)) * 0.0625;
    }

    return fract(totalPosition.zx);
}

void main() {
    float u = (textureData & 15) * 0.0625;
    float v = (textureData >> 4 & 15) * 0.0625;
    vec4 color = texture(textureSampler, vec2(u, v) + getUVOffset(textureData >> 8 & 7));
    if (color.a == 0.0f) {
        discard;
    }

    float absTime = abs(time);
    float skyLight = getSkyLight();
    float blockLight = getBlockLight();

    float sunIllumination = dot(normal, sunDirection) * 0.2 * skyLight * absTime;
    float timeLight = max(0.2, easeInOutQuart(absTime));
    float nightLight = 0.6 * (1 - absTime) * (1 - absTime);
    float light = max(blockLight + 0.2, max(0.2, skyLight) * timeLight + sunIllumination);
    vec3 fragLight = vec3(light, light, max(blockLight + 0.2, max(0.2, skyLight + nightLight) * timeLight + sunIllumination));
    float distance = length(cameraPosition - totalPosition);

    float waterFogMultiplier = min(1, isFlag(HEAD_UNDER_WATER_BIT) * max(0.5, distance * 0.000625));
    fragColor = vec4(color.rgb * fragLight * (1 - waterFogMultiplier) + vec3(0.0, 0.098, 0.643) * waterFogMultiplier * timeLight, color.a);
}
