package terrascape.player;

import org.joml.Vector2i;
import terrascape.server.Material;
import terrascape.server.Launcher;
import terrascape.entity.GUIElement;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

public final class MouseInput {

    public MouseInput(Player player) {
        this.player = player;
        previousPos = new Vector2i(0, 0);
        currentPos = new Vector2i(0, 0);
    }

    public void init() {
        GLFW.glfwSetCursorPosCallback(Launcher.getWindow().getWindow(), (long window, double xPos, double yPos) -> {
            currentPos.x = (int) xPos;
            currentPos.y = (int) yPos;

            playHoverSelectionSound();
        });

        GLFW.glfwSetMouseButtonCallback(Launcher.getWindow().getWindow(), (long window, int button, int action, int mods) -> {
            player.handleNonMovementInputs(button | IS_MOUSE_BUTTON, action);
            player.getInteractionHandler().input(button | IS_MOUSE_BUTTON, action);
        });

        GLFW.glfwSetScrollCallback(Launcher.getWindow().getWindow(), (long window, double xPos, double yPos) -> {
            if (player.isInInventory()) {
                float scrollValue = (float) yPos * -0.05f;
                player.updateInventoryScroll(scrollValue);
            } else if (Launcher.getWindow().isKeyPressed(ZOOM_BUTTON)) {
                player.getCamera().changeZoomModifier(yPos > 0 ? 0.9f : 1 / 0.9f);
            } else if (SCROLL_HOT_BAR) {
                player.setSelectedHotBarSlot((player.getSelectedHotBarSlot() - (int) yPos + 9) % 9);
            }

            playHoverSelectionSound();
        });

        GLFW.glfwSetInputMode(Launcher.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        updateSettings();
    }

    public void input() {
        float deltaX = currentPos.x - previousPos.x;
        float deltaY = currentPos.y - previousPos.y;

        float sensitivityFactor = MOUSE_SENSITIVITY * 0.6f + 0.2f;
        sensitivityFactor = 1.2f * sensitivityFactor * sensitivityFactor * sensitivityFactor;
        float rotationX = deltaX * sensitivityFactor;
        float rotationY = deltaY * sensitivityFactor;

        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;

        if (!player.isInInventory()) player.getCamera().moveRotation(rotationX, rotationY);
    }


    public int getX() {
        return currentPos.x;
    }

    public int getY() {
        return currentPos.y;
    }

    public void updateSettings() {
        if (GLFW.glfwRawMouseMotionSupported()) {
            if (RAW_MOUSE_INPUT)
                GLFW.glfwSetInputMode(Launcher.getWindow().getWindow(), GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
            else
                GLFW.glfwSetInputMode(Launcher.getWindow().getWindow(), GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_FALSE);
        }
    }

    private void playHoverSelectionSound() {
        if (!player.isInInventory()) return;
        byte currentHoveredOverMaterial = GUIElement.getHoveredOverMaterial(player.getInventoryScroll());
        if (currentHoveredOverMaterial == hoveredOverMaterial) return;

        Vector3f position = player.getCamera().getPosition();
        hoveredOverMaterial = currentHoveredOverMaterial;
        Launcher.getSound().playRandomSound(Material.getFootstepsSound(hoveredOverMaterial),
                position.x, position.y, position.z, 0.0f, 0.0f, 0.0f, INVENTORY_GAIN);
    }

    private final Vector2i previousPos, currentPos;
    private final Player player;
    private byte hoveredOverMaterial = AIR;
}
