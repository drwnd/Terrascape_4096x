#version 400 core

in vec2 fragTextureCoordinates;

out vec4 fragColor;

uniform sampler2D textureAtlas1;
uniform sampler2D textureAtlas2;
uniform float time;

void main() {
    vec4 color1 = texture(textureAtlas1, fragTextureCoordinates);
    vec4 color2 = texture(textureAtlas2, fragTextureCoordinates);

    float absTime = abs(time);
    fragColor = color1 * absTime + color2 * (1 - absTime);
}
