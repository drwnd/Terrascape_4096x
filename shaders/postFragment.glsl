#version 400 core

uniform sampler2D colorTexture;
uniform sampler2D depthTexture;

uniform mat4 projectionMatrix;
uniform mat4 projectionInverse;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

const float RADIUS = 4;
const float MAX_DISTANCE = 1000.0;
const vec2 HALF_2 = vec2(0.5);
const int SAMPLE_COUNT = 30;
const vec3[SAMPLE_COUNT] SAMPLES = vec3[SAMPLE_COUNT](
vec3(0.017003436, -0.05776007, 0.07984145),
vec3(-0.07552401, 0.046140663, 0.048663773),
vec3(-0.04431546, 0.079915516, 0.049655307),
vec3(0.039508887, 0.062442634, 0.08013091),
vec3(-0.022547528, 0.057572074, 0.098148175),
vec3(-0.111611985, -0.026368897, 0.049723715),
vec3(0.057506688, -0.10244457, 0.06851347),
vec3(0.11282974, -0.042618874, 0.08748761),
vec3(0.083980955, -0.041029043, 0.13475835),
vec3(-0.056649562, 0.17178239, 0.006529227),
vec3(-0.14791137, 0.056057476, 0.12239196),
vec3(0.21522273, -0.0054672663, 0.049902838),
vec3(-0.21953881, 0.0798086, 0.07049331),
vec3(0.15623187, -0.21586448, 0.036812328),
vec3(-0.19698387, 0.14680848, 0.16510794),
vec3(0.22838704, 0.1334328, 0.18883866),
vec3(-0.026556054, -0.317221, 0.15937899),
vec3(-0.33738846, 0.193117, 0.013994539),
vec3(-0.15644412, 0.33256143, 0.21143351),
vec3(-0.30153123, -0.127244, 0.32466733),
vec3(0.27573106, -0.28608266, 0.30352774),
vec3(-0.38244802, -0.36117476, 0.12636176),
vec3(-0.31014502, -0.31854373, 0.37867656),
vec3(-0.21005128, 0.2667588, 0.5294896),
vec3(0.6012385, 0.300595, 0.0716309),
vec3(0.09396837, -0.39903095, 0.5979709),
vec3(-0.60251135, -0.035135813, 0.48777217),
vec3(0.69732004, -0.21059868, 0.39576992),
vec3(-0.18346782, -0.42507255, 0.7530663),
vec3(0.497895, -0.5835566, 0.5450167)
);

// Code from https://medium.com/better-programming/depth-only-ssao-for-forward-renderers-1a3dcfa1873a
vec3 calcViewPosition(vec2 coords) {
    float fragmentDepth = texture(depthTexture, coords).r;

    vec4 ndc = vec4(coords.x * 2.0 - 1.0, coords.y * 2.0 - 1.0, fragmentDepth * 2.0 - 1.0, 1.0);

    vec4 vs_pos = projectionInverse * ndc;
    vs_pos.xyz = vs_pos.xyz / vs_pos.w;

    return vs_pos.xyz;
}

float computeOcclusion() {
    vec3 viewPos = calcViewPosition(fragTextureCoordinate);

    vec3 viewNormal = cross(dFdy(viewPos.xyz), dFdx(viewPos.xyz));

    viewNormal = normalize(viewNormal * -1.0);
    vec3 randomVec = vec3(1, 1, 0);
    vec3 tangent = normalize(randomVec - viewNormal * dot(randomVec, viewNormal));
    vec3 bitangent = cross(viewNormal, tangent);
    mat3 TBN = mat3(tangent, bitangent, viewNormal);
    float occlusion_factor = 0.0;

    for (int i = 0; i < SAMPLE_COUNT; i++) {
        vec3 samplePos = TBN * SAMPLES[i];

        samplePos = viewPos + samplePos * RADIUS;

        vec4 offset = vec4(samplePos, 1.0);
        offset = projectionMatrix * offset;
        offset.xy /= offset.w;
        offset.xy = offset.xy * HALF_2 + HALF_2;

        float geometryDepth = calcViewPosition(offset.xy).z;
        float rangeCheck = float(abs(viewPos.z - geometryDepth) < RADIUS);
        float distanceScale = max(0.1, 1 - smoothstep(0.0, MAX_DISTANCE, min(MAX_DISTANCE, abs(geometryDepth))));

        occlusion_factor += float(geometryDepth >= samplePos.z + 0.0001) * rangeCheck * distanceScale;
    }

    float average_occlusion_factor = occlusion_factor * (1.0 / SAMPLE_COUNT);
    float visibility_factor = 1.0 - average_occlusion_factor;

    visibility_factor = pow(visibility_factor, 1.5);

    return visibility_factor;
}

void main() {
    vec4 color = texture(colorTexture, fragTextureCoordinate);
    fragColor = color * computeOcclusion();
}