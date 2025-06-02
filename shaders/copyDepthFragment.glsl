#version 150

uniform sampler2D depthTexture;
in vec2 fragTextureCoordinate;

void main( void )
{
    gl_FragDepth = texture(depthTexture, fragTextureCoordinate).r;
}
