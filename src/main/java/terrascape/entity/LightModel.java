package terrascape.entity;

import org.joml.Vector3i;

public record LightModel(int buffer, int x, int y, int z, int lod, int count) {

    public LightModel(int buffer, Vector3i position, int lod, int count) {
        this(buffer, position.x, position.y, position.z, lod, count);
    }
}
