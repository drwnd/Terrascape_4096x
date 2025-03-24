#version 400 core

flat in int material;
in float blockLight;
in float skyLight;
in float ambientOcclusionLevel;
in vec3 normal;
in vec3 totalPosition;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform int headUnderWater;
uniform float time;
uniform vec3 cameraPosition;

vec3 getSunDirection() {
    float alpha = time * 3.1415926536;
    float sinAlpha = sin(alpha);
    float cosAlpha = cos(alpha);
    return vec3(cosAlpha - sinAlpha, -0.3, cosAlpha + sinAlpha);
}

float easeInOutQuart(float x) {
    //x < 0.5 ? 8 * x * x * x * x : 1 - pow(-2 * x + 2, 4) / 2;
    float inValue = 8.0 * x * x * x * x;
    float outValue = 1.0 - pow(-2.0 * x + 2.0, 4.0) / 2.0;
    return step(inValue, 0.5) * inValue + step(0.5, outValue) * outValue;
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

void main(){
    float u = (material >> 4 & 15) * 0.0625;
    float v = (material & 15) * 0.0625;
    vec4 color = texture(textureSampler, vec2(u, v) + getUVOffset(material >> 8 & 7));

    float distance = length(cameraPosition - totalPosition);
    float angle = abs(dot((totalPosition - cameraPosition) / distance, normal));

    vec3 waterColor = color.rgb + angle * vec3(0.0, 0.4, 0.15);

    vec3 sunDirection = getSunDirection();
    float absTime = abs(time);
    float sunIllumination = dot(normal, sunDirection) * 0.2 * skyLight * absTime;

    float timeLight = max(0.2, easeInOutQuart(absTime));
    float nightLight = -0.6 * (1 - absTime) * (1 - absTime);
    float light = max(blockLight + 0.2, max(0.2, skyLight) * timeLight + sunIllumination) * ambientOcclusionLevel;
    vec3 fragLight = vec3(light, light, max(blockLight + 0.2, max(0.2, skyLight + nightLight) * timeLight + sunIllumination) * ambientOcclusionLevel);

    float waterFogMultiplier = min(1, headUnderWater * max(0.5, distance * 0.000625));
    fragColor = vec4(waterColor * fragLight * (1 - waterFogMultiplier) + vec3(0.0, 0.098, 0.643) * waterFogMultiplier * timeLight, color.a - angle * 0.3);
}
