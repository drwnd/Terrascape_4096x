#version 400 core

uniform sampler2D normalTexture;
uniform sampler2D positionTexture;
uniform sampler2D colorTexture;
uniform sampler2D ssaoTexture;
uniform sampler2D lightTexture;
uniform sampler2D propertiesTexture;
uniform sampler2D shadowTexture;

uniform ivec2 screenSize;
uniform int flags;
uniform float time;
uniform vec3 cameraPosition;
uniform vec3 sunDirection;
uniform mat4 sunMatrix;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

const int HEAD_UNDER_WATER_BIT = 1;
const float NIGHT_BRIGHTNESS = 0.2;

float easeInOutQuart(float x) {
    float inValue = 8.0 * x * x * x * x;
    float outValue = 1.0 - pow(-2.0 * x + 2.0, 4.0) * 0.5;
    return step(inValue, 0.5) * inValue + step(0.5, outValue) * outValue;
}

int isFlag(int bit) {
    return int((flags & bit) != 0);
}

float getSkyLight(vec3 position, vec3 normal) {
    vec4 shadowCoord = sunMatrix * vec4(floor(position - normal * 0.5), 1);
    shadowCoord.xyz /= shadowCoord.w;
    shadowCoord = shadowCoord * 0.5 + 0.5;

    float closestDepth = texture(shadowTexture, shadowCoord.xy).r;
    float currentDepth = shadowCoord.z;

    return currentDepth - 0.007  > closestDepth ? 0.0 : 1.0;
}

float getAmbientOcclusion() {
    float occlusion = 0.0;
    vec2 texelSize = vec2(1.0 / float(screenSize.x), 1.0 / float(screenSize.y));

    for (int x = -2; x < 2; x++) {
        for (int y = -2; y < 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            occlusion += texture(ssaoTexture, fragTextureCoordinate + offset).r;
        }
    }
    return occlusion * 0.0625;
}

void main() {
    vec4 color = texture(colorTexture, fragTextureCoordinate);
    if (color.a == 0.0f) discard;

    vec3 normal = texture(normalTexture, fragTextureCoordinate).xyz;
    vec3 position = texture(positionTexture, fragTextureCoordinate).xyz;
    vec3 blockLight = texture(lightTexture, fragTextureCoordinate).rgb;
    float emissivness = texture(propertiesTexture, fragTextureCoordinate).r;

    float absTime = abs(time);
    float skyLight = getSkyLight(position, normal);
    float occlusion = getAmbientOcclusion();

    float sunIllumination = dot(normal, sunDirection) * 0.2 * skyLight * absTime;
    float timeLight = max(NIGHT_BRIGHTNESS, easeInOutQuart(absTime));
    float nightLight = 0.6 * (1 - absTime) * (1 - absTime);
    float light = max(NIGHT_BRIGHTNESS, max(NIGHT_BRIGHTNESS, skyLight) * timeLight + sunIllumination);
    vec3 fragLight = max(vec3(emissivness), vec3(light, light, max(NIGHT_BRIGHTNESS, max(NIGHT_BRIGHTNESS, skyLight + nightLight) * timeLight + sunIllumination)));
    float distance = length(cameraPosition - position);
    float waterFogMultiplier = min(1, isFlag(HEAD_UNDER_WATER_BIT) * max(0.5, distance * 0.000625));

    vec3 baseColor = color.rgb * fragLight * (1 - waterFogMultiplier);
    vec3 waterColor = vec3(0.0, 0.098, 0.643) * waterFogMultiplier * timeLight;
    vec3 lightColor = color.rgb * blockLight;
    fragColor = vec4(baseColor + waterColor + lightColor, color.a);

    fragColor *= occlusion;

//    fragColor = vec4(texture(shadowTexture, fragTextureCoordinate).r);
//    if (fragColor.r != 0) fragColor = vec4(1);
}