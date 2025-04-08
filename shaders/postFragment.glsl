#version 400 core

uniform sampler2D colorTexture;
uniform sampler2D ssaoTexture;

uniform ivec2 screenSize;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

void main() {
    float occlusion = 0.0;
    vec2 texelSize = vec2(1.0 / float(screenSize.x), 1.0 / float(screenSize.y));

    for (int x = -2; x < 2; x++) {
        for (int y = -2; y < 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            occlusion += texture(ssaoTexture, fragTextureCoordinate + offset).r;
        }
    }
    occlusion = occlusion * 0.0625;
//    float vignette = pow(1 - length(fragTextureCoordinate - vec2(0.5, 0.5)), 0.4);

    fragColor = texture(colorTexture, fragTextureCoordinate) * occlusion;
}