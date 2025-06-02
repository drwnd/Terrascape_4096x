#version 400 core

uniform sampler2D textureAtlas;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

void main() {
    vec4 color = texture(textureAtlas, fragTextureCoordinate);
    if(color.a == 0.0){
        discard;
    }
    fragColor = color;
}
