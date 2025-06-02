#version 400 core

in vec2 textureCoordinates;

out vec4 fragColor;

uniform sampler2D textureAtlas;
uniform vec3 color;

void main() {
    fragColor = texture(textureAtlas, textureCoordinates);
    if (fragColor.a == 0.0)discard;
    if (fragColor.a == 1.0) fragColor *= vec4(color, 1.0);
}