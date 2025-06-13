package terrascape.player;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;
import terrascape.server.ServerLogic;

public final class WindowManager {

    public WindowManager(String title, int width, int height, boolean vSync, boolean maximized) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
        this.maximized = maximized;

        projectionMatrix = new Matrix4f();
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        createWindow();
        GL.createCapabilities();

        GL46.glClearColor(0, 0, 0, 1);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDepthFunc(GL46.GL_LESS);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glCullFace(GL46.GL_BACK);
    }

    private void createWindow() {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL46.GL_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL46.GL_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL46.GL_TRUE);

        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (vidMode == null) throw new RuntimeException("Could not get video mode");

        if (maximized) {
            window = GLFW.glfwCreateWindow(vidMode.width(), vidMode.height(), title, GLFW.glfwGetPrimaryMonitor(), MemoryUtil.NULL);
            width = vidMode.width();
            height = vidMode.height();
        } else {
            window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
            GLFW.glfwSetWindowPos(window, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
        }

        if (window == MemoryUtil.NULL) throw new RuntimeException("Failed to create GLFW window");

        GLFW.glfwSetFramebufferSizeCallback(window, (long window, int width, int height) -> {
            this.width = width;
            this.height = height;
            updateProjectionMatrix();
            if (width != 0 && height != 0) ServerLogic.getPlayer().getRenderer().resize();
        });

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(vSync ? 1 : 0);
        GLFW.glfwShowWindow(window);

        updateProjectionMatrix();
    }

    public void toggleFullScreen() {
        maximized = !maximized;
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (vidMode == null) throw new RuntimeException("Could not get video mode");

        if (maximized) GLFW.glfwSetWindowMonitor(window, GLFW.glfwGetPrimaryMonitor(), 0, 0, vidMode.width(), vidMode.height(), GLFW.GLFW_DONT_CARE);
        else GLFW.glfwSetWindowMonitor(window, MemoryUtil.NULL, 0, 0, width, height, GLFW.GLFW_DONT_CARE);
    }

    public void update() {
        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
    }

    public void cleanUp() {
        GLFW.glfwDestroyWindow(window);
    }

    public boolean isKeyPressed(int keycode) {
        if ((keycode & IS_MOUSE_BUTTON) == 0) return GLFW.glfwGetKey(window, keycode & 0x7FFFFFFF) == GLFW.GLFW_PRESS;
        else return GLFW.glfwGetMouseButton(window, keycode & 0x7FFFFFFF) == GLFW.GLFW_PRESS;
    }

    public boolean windowShouldClose() {
        return GLFW.glfwWindowShouldClose(window);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getWindow() {
        return window;
    }

    public void updateProjectionMatrix() {
        float aspectRatio = (float) width / height;
        projectionMatrix.setPerspective(FOV, aspectRatio, Z_NEAR, Z_FAR);
    }

    public void updateProjectionMatrix(float fov) {
        float aspectRatio = (float) width / height;
        projectionMatrix.setPerspective(fov, aspectRatio, Z_NEAR, Z_FAR);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    private final String title;
    private int width, height;
    private long window;

    private final boolean vSync;
    private boolean maximized;

    private final Matrix4f projectionMatrix;
}
