package terrascape.utils;

import terrascape.player.Camera;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import terrascape.player.WindowManager;

import static terrascape.utils.Constants.*;

public final class Transformation {

    public static Matrix4f createSkyBoxTransformationMatrix(Camera camera, WindowManager window) {
        Vector2f rot = camera.getRotation();

        Matrix4f matrix = new Matrix4f(window.getProjectionMatrix());
        matrix.rotate((float) Math.toRadians(rot.x), X_AXIS).rotate((float) Math.toRadians(rot.y), Y_AXIS);

        return matrix;
    }

    public static Matrix4f getProjectionViewMatrix(Camera camera, WindowManager window) {
        Vector3f pos = camera.getPosition();
        Vector2f rot = camera.getRotation();

        Matrix4f matrix = new Matrix4f(window.getProjectionMatrix());
        matrix.rotate((float) Math.toRadians(rot.x), X_AXIS).rotate((float) Math.toRadians(rot.y), Y_AXIS);
        matrix.translate(
                -Utils.fraction(pos.x / CHUNK_SIZE) * CHUNK_SIZE,
                -Utils.fraction(pos.y / CHUNK_SIZE) * CHUNK_SIZE,
                -Utils.fraction(pos.z / CHUNK_SIZE) * CHUNK_SIZE);

        return matrix;
    }

    public static Matrix4f getFrustumCullingMatrix(Camera camera, WindowManager window) {
        Vector3f pos = camera.getPosition();
        Vector2f rot = camera.getRotation();

        Matrix4f matrix = new Matrix4f(window.getProjectionMatrix());
        matrix.rotate((float) Math.toRadians(rot.x), X_AXIS).rotate((float) Math.toRadians(rot.y), Y_AXIS);
        matrix.translate(-pos.x, -pos.y, -pos.z);

        return matrix;
    }

    public static Matrix4f getSunMatrix(Vector3f sunDirection) {
        Matrix4f matrix = new Matrix4f();
        matrix.ortho(-SHADOW_RANGE, SHADOW_RANGE, -SHADOW_RANGE, SHADOW_RANGE, 100, SHADOW_RANGE * 2);
        matrix.lookAt(-sunDirection.x * SHADOW_RANGE, -sunDirection.y * SHADOW_RANGE, -sunDirection.z * SHADOW_RANGE,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f);
        return matrix;
    }

    public static Vector3f getSunDirection(float renderTime) {
        float alpha = (float) (renderTime * Math.PI);

        return new Vector3f(
                (float) Math.sin(alpha),
                (float) Math.min(Math.cos(alpha), -0.3),
                (float) Math.sin(alpha)
        ).normalize();
    }

    private Transformation() {
    }

    private static final Vector3f X_AXIS = new Vector3f(1.0f, 0.0f, 0.0f);
    private static final Vector3f Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
}
