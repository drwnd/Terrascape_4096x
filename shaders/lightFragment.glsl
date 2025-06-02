#version 400 core

flat in ivec3 centerPosition;
flat in vec3 color;
flat in float squareRadius;
flat in float strength;

out vec3 light;

uniform sampler2D normalTexture;
uniform sampler2D positionTexture;
uniform ivec2 screenSize;

const float BRIGHTNESS_MULTIPLIER = 64.0;

void main() {
    vec2 uvCoord = gl_FragCoord.xy / screenSize;
    vec3 normal = texture(normalTexture, uvCoord).xyz;
    vec3 position = floor(texture(positionTexture, uvCoord).xyz - 0.9 * normal);

    vec3 offset = centerPosition - position;
    float exposure = dot(normalize(offset), normal);
    float squareDistace = (offset.x * offset.x + offset.y * offset.y + offset.z * offset.z);

    if (exposure < 0 || squareDistace > squareRadius) discard;

    float edgeFallOff = pow(squareDistace / squareRadius, 8);
    float falloff = min(1.0, BRIGHTNESS_MULTIPLIER * strength / squareDistace) * clamp(1 - edgeFallOff, 0.0, 1.0);

    light = color * exposure * falloff;
}
