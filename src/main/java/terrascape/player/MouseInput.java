package terrascape.player;

import terrascape.server.Material;
import terrascape.server.Launcher;
import terrascape.entity.GUIElement;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

public final class MouseInput {

    public MouseInput(Player player) {
        this.player = player;
        previousPos = new Vector2f(0, 0);
        currentPos = new Vector2f(0, 0);
        displayVec = new Vector2f();
    }

    public void init() {
        GLFW.glfwSetCursorPosCallback(Launcher.getWindow().getWindow(), (long window, double xPos, double yPos) -> {
            currentPos.x = (float) xPos;
            currentPos.y = (float) yPos;

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
    }

    public void input() {
        float x = currentPos.x - previousPos.x;
        float y = currentPos.y - previousPos.y;

        // This is correct
        displayVec.y = x;
        displayVec.x = y;

        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;
    }

    public Vector2f getDisplayVec() {
        Vector2f returns = displayVec;
        displayVec = new Vector2f();
        return returns;
    }

    public int getX() {
        return (int) (currentPos.x);
    }

    public int getY() {
        return (int) (currentPos.y);
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

    private final Vector2f previousPos, currentPos;
    private Vector2f displayVec;
    private final Player player;
    private byte hoveredOverMaterial = AIR;
}
