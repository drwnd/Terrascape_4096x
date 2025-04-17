package terrascape.player;

import org.joml.Vector2f;
import org.joml.Vector3f;
import terrascape.server.Launcher;

import static terrascape.utils.Settings.FOV;

public final class Camera {

    public Camera() {
        position = new Vector3f(0.0f, 0.0f, 0.0f);
        rotation = new Vector2f(0.0f, 0.0f);
    }

    public void changeZoomModifier(float multiplier) {
        zoomModifier = Math.min(1.0f, zoomModifier * multiplier);
        Launcher.getWindow().updateProjectionMatrix(FOV * zoomModifier);
    }

    public void movePosition(float x, float y, float z) {
        position.add(x, y, z);
    }

    public Vector3f getDirection() {

        float rotationXRadians = (float) Math.toRadians(rotation.y);
        float rotationYRadians = (float) Math.toRadians(rotation.x);

        float x = (float) Math.sin(rotationXRadians);
        float y = (float) -Math.sin(rotationYRadians);
        float z = (float) -Math.cos(rotationXRadians);

        float v = (float) Math.sqrt(1 - y * y);

        x *= v;
        z *= v;

        return new Vector3f(x, y, z);
    }

    public void setPosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
    }

    public void moveRotation(float yaw, float pitch) {
        rotation.x += pitch;
        rotation.y += yaw;

        rotation.x = Math.max(-90, Math.min(rotation.x, 90));
        rotation.y %= 360.0f;
    }

    public void setRotation(float x, float y) {
        rotation.x = x;
        rotation.y = y;
    }

    public void setZoomModifier(float modifier) {
        this.zoomModifier = modifier;
        Launcher.getWindow().updateProjectionMatrix(FOV * zoomModifier);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector2f getRotation() {
        return rotation;
    }

    private final Vector3f position;
    private final Vector2f rotation;
    private float zoomModifier = 1.0f;
}
