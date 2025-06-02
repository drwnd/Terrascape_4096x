#version 400 core

flat in int textureData;
flat in vec3 normal;
in vec3 totalPosition;

layout(location = 0) out vec3 fragNormal;
layout(location = 1) out vec3 fragPosition;
layout(location = 2) out vec4 fragColor;
layout(location = 3) out float fragProperties;

uniform sampler2D textureAtlas;
uniform sampler2D propertiesTexture;

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
    vec2 uvCoord = vec2(u, v) + getUVOffset(textureData >> 8 & 7);
    fragColor = texture(textureAtlas, uvCoord);
    fragProperties = texture(propertiesTexture, uvCoord).r;

    if (fragColor.a == 0) discard;

    fragPosition = totalPosition;
    fragNormal = normal;
}
