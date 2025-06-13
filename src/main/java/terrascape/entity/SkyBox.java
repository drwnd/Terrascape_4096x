package terrascape.entity;

import org.joml.Vector3f;

public record SkyBox(int vao, int vertexCount, Vector3f position, Texture dayTexture, Texture nightTexture) {

}
