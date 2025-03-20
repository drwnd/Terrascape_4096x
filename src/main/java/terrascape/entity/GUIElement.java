package terrascape.entity;

import terrascape.player.Player;
import terrascape.server.Material;
import terrascape.player.ObjectLoader;
import terrascape.player.WindowManager;
import terrascape.server.Launcher;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.GUI_SIZE;

public final class GUIElement {

    public GUIElement(int vao, int vertexCount, int vbo1, int vbo2, Vector2f position) {
        this.vao = vao;
        this.vbo1 = vbo1;
        this.vbo2 = vbo2;
        this.vertexCount = vertexCount;
        this.position = position;
    }

    public int getVao() {
        return vao;
    }

    public int getVbo1() {
        return vbo1;
    }

    public int getVbo2() {
        return vbo2;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Vector2f getPosition() {
        return position;
    }

    public void setPosition(Vector2f position) {
        this.position = position;
    }

    public static float[] getCrossHairVertices() {
        WindowManager window = Launcher.getWindow();

        int width = window.getWidth();
        int height = window.getHeight();

        return new float[]{
                -16.0f * GUI_SIZE / width, 16.0f * GUI_SIZE / height,
                -16.0f * GUI_SIZE / width, -16.0f * GUI_SIZE / height,
                16.0f * GUI_SIZE / width, 16.0f * GUI_SIZE / height,

                -16.0f * GUI_SIZE / width, -16.0f * GUI_SIZE / height,
                16.0f * GUI_SIZE / width, -16.0f * GUI_SIZE / height,
                16.0f * GUI_SIZE / width, 16.0f * GUI_SIZE / height};
    }

    public static float[] getMaterialDisplayVertices(byte block) {
        if (block == 0) return new float[]{};
        WindowManager window = Launcher.getWindow();

        final int width = window.getWidth();
        final int height = window.getHeight();

        final float sin30 = (float) Math.sin(Math.toRadians(30)) * GUI_SIZE;
        final float cos30 = (float) Math.cos(Math.toRadians(30)) * GUI_SIZE;

        float[] vertices = new float[36];

        float widthX = 16 * GUI_SIZE;
        float widthY = 16 * GUI_SIZE;
        float widthZ = 16 * GUI_SIZE;

        float startX = 0;
        float startY = 0;

        float rightCornersX = startX + cos30 * 16.0f / width;
        float leftCornersX = startX - cos30 * 16.0f / width;
        float backCornerX = rightCornersX + leftCornersX - 0.0f;

        float bottomRightCornerY = startY + sin30 * widthX / height;
        float topRightCornerY = startY + widthY / height + sin30 * widthX / height;
        float bottomLeftCornerY = startY + sin30 * widthZ / height;
        float topLeftCornerY = startY + widthY / height + sin30 * widthZ / height;
        float backCornerY = startY + widthY / height + sin30 * widthZ / height + sin30 * widthX / height;
        float centerCornerY = startY + widthY / height;

        vertices[0] = startX;
        vertices[1] = startY;
        vertices[2] = startX;
        vertices[3] = centerCornerY;
        vertices[4] = rightCornersX;
        vertices[5] = bottomRightCornerY;
        vertices[6] = startX;
        vertices[7] = centerCornerY;
        vertices[8] = rightCornersX;
        vertices[9] = topRightCornerY;
        vertices[10] = rightCornersX;
        vertices[11] = bottomRightCornerY;

        vertices[12] = leftCornersX;
        vertices[13] = topLeftCornerY;
        vertices[14] = backCornerX;
        vertices[15] = backCornerY;
        vertices[16] = startX;
        vertices[17] = centerCornerY;
        vertices[18] = backCornerX;
        vertices[19] = backCornerY;
        vertices[20] = rightCornersX;
        vertices[21] = topRightCornerY;
        vertices[22] = startX;
        vertices[23] = centerCornerY;

        vertices[24] = startX;
        vertices[25] = startY;
        vertices[26] = startX;
        vertices[27] = centerCornerY;
        vertices[28] = leftCornersX;
        vertices[29] = bottomLeftCornerY;
        vertices[30] = startX;
        vertices[31] = centerCornerY;
        vertices[32] = leftCornersX;
        vertices[33] = topLeftCornerY;
        vertices[34] = leftCornersX;
        vertices[35] = bottomLeftCornerY;
        return vertices;
    }

    public static float[] getMaterialDisplayTextureCoordinates(int textureIndexFront, int textureIndexTop, int textureIndexRight, byte block) {
        if (block == 0) return new float[]{};
        float[] textureCoordinates = new float[36];

        final int textureFrontU = textureIndexFront & 15;
        final int textureFrontV = (textureIndexFront >> 4) & 15;
        final int textureTopU = textureIndexTop & 15;
        final int textureTopV = (textureIndexTop >> 4) & 15;
        final int textureLeftU = textureIndexRight & 15;
        final int textureLeftV = (textureIndexRight >> 4) & 15;

        final float upperFrontU = (textureFrontU + 1) * 0.0625f;
        final float lowerFrontU = (textureFrontU) * 0.0625f;
        final float upperFrontV = (textureFrontV) * 0.0625f;
        final float lowerFrontV = (textureFrontV + 1) * 0.0625f;
        textureCoordinates[0] = lowerFrontU;
        textureCoordinates[1] = lowerFrontV;
        textureCoordinates[2] = lowerFrontU;
        textureCoordinates[3] = upperFrontV;
        textureCoordinates[4] = upperFrontU;
        textureCoordinates[5] = lowerFrontV;
        textureCoordinates[6] = lowerFrontU;
        textureCoordinates[7] = upperFrontV;
        textureCoordinates[8] = upperFrontU;
        textureCoordinates[9] = upperFrontV;
        textureCoordinates[10] = upperFrontU;
        textureCoordinates[11] = lowerFrontV;

        final float upperTopU = (textureTopU + 1) * 0.0625f;
        final float lowerTopU = (textureTopU) * 0.0625f;
        final float upperTopV = (textureTopV) * 0.0625f;
        final float lowerTopV = (textureTopV + 1) * 0.0625f;
        textureCoordinates[12] = lowerTopU;
        textureCoordinates[13] = lowerTopV;
        textureCoordinates[14] = lowerTopU;
        textureCoordinates[15] = upperTopV;
        textureCoordinates[16] = upperTopU;
        textureCoordinates[17] = lowerTopV;
        textureCoordinates[18] = lowerTopU;
        textureCoordinates[19] = upperTopV;
        textureCoordinates[20] = upperTopU;
        textureCoordinates[21] = upperTopV;
        textureCoordinates[22] = upperTopU;
        textureCoordinates[23] = lowerTopV;

        final float upperRightU = (textureLeftU) * 0.0625f;
        final float lowerRightU = (textureLeftU + 1) * 0.0625f;
        final float upperRightV = (textureLeftV) * 0.0625f;
        final float lowerRightV = (textureLeftV + 1) * 0.0625f;
        textureCoordinates[24] = lowerRightU;
        textureCoordinates[25] = lowerRightV;
        textureCoordinates[26] = lowerRightU;
        textureCoordinates[27] = upperRightV;
        textureCoordinates[28] = upperRightU;
        textureCoordinates[29] = lowerRightV;
        textureCoordinates[30] = lowerRightU;
        textureCoordinates[31] = upperRightV;
        textureCoordinates[32] = upperRightU;
        textureCoordinates[33] = upperRightV;
        textureCoordinates[34] = upperRightU;
        textureCoordinates[35] = lowerRightV;

        return textureCoordinates;
    }

    public static float[] getHotBarVertices() {
        WindowManager window = Launcher.getWindow();

        int width = window.getWidth();
        int height = window.getHeight();
        float sizeX = 180;
        float sizeY = 40;

        return new float[]{
                -sizeX * GUI_SIZE / width, -0.5f,
                -sizeX * GUI_SIZE / width, sizeY * GUI_SIZE / height - 0.5f,
                sizeX * GUI_SIZE / width, -0.5f,
                -sizeX * GUI_SIZE / width, sizeY * GUI_SIZE / height - 0.5f,
                sizeX * GUI_SIZE / width, sizeY * GUI_SIZE / height - 0.5f,
                sizeX * GUI_SIZE / width, -0.5f
        };
    }

    public static float[] getHotBarSelectionIndicatorVertices() {
        WindowManager window = Launcher.getWindow();

        int width = window.getWidth();
        int height = window.getHeight();

        float sizeX = 24;
        float sizeY = 48;

        float yOffset = 4 * GUI_SIZE / height;

        return new float[]{
                -sizeX * GUI_SIZE / width, -0.5f - yOffset,
                -sizeX * GUI_SIZE / width, sizeY * GUI_SIZE / height - 0.5f - yOffset,
                sizeX * GUI_SIZE / width, -0.5f - yOffset,
                -sizeX * GUI_SIZE / width, sizeY * GUI_SIZE / height - 0.5f - yOffset,
                sizeX * GUI_SIZE / width, sizeY * GUI_SIZE / height - 0.5f - yOffset,
                sizeX * GUI_SIZE / width, -0.5f - yOffset
        };
    }

    public static byte getHoveredOverMaterial(float inventoryScroll) {
        double[] xPos = new double[1];
        double[] yPos = new double[1];
        WindowManager window = Launcher.getWindow();
        GLFW.glfwGetCursorPos(window.getWindow(), xPos, yPos);
        double x = xPos[0] / window.getWidth() - 0.5;
        double y = yPos[0] / window.getHeight() - 0.5;

        y += inventoryScroll;

        int valueY = (int) ((y - 0.005 * GUI_SIZE + 0.5) / (0.04 * GUI_SIZE));
        valueY = Math.min(122, Math.max(valueY, 1));

        return (byte) (valueY);
    }

    public static void generateInventoryElements(ArrayList<GUIElement> elements, Texture atlas) {
        for (int baseMaterial = 1; baseMaterial < 123; baseMaterial++) {

            byte material = (byte) (baseMaterial);
            float[] vertices = GUIElement.getMaterialDisplayVertices(material);

            int textureIndexFront = Material.getTextureIndex(material, 0);
            int textureIndexTop = Material.getTextureIndex(material, 1);
            int textureIndexLeft = Material.getTextureIndex(material, 5);
            float[] textureCoordinates = GUIElement.getMaterialDisplayTextureCoordinates(textureIndexFront, textureIndexTop, textureIndexLeft, material);
            GUIElement element = ObjectLoader.loadGUIElement(vertices, textureCoordinates, new Vector2f(0.5f - 0.02f * GUI_SIZE, 0.5f - GUI_SIZE * 0.04f * (1 + baseMaterial)));
            element.setTexture(atlas);
            elements.add(element);
        }
    }

    public static void loadGUIElements(Player player) throws Exception {
        ArrayList<GUIElement> GUIElements = player.getGUIElements();
        GUIElement crossHair = ObjectLoader.loadGUIElement(GUIElement.getCrossHairVertices(), GUI_ELEMENT_TEXTURE_COORDINATES, new Vector2f(0.0f, 0.0f));
        crossHair.setTexture(new Texture(ObjectLoader.loadTexture("textures/CrossHair.png")));
        GUIElements.addFirst(crossHair);

        GUIElement hotBarGUIElement = ObjectLoader.loadGUIElement(GUIElement.getHotBarVertices(), GUI_ELEMENT_TEXTURE_COORDINATES, new Vector2f(0.0f, 0.0f));
        hotBarGUIElement.setTexture(new Texture(ObjectLoader.loadTexture("textures/HotBar.png")));
        GUIElements.add(1, hotBarGUIElement);

        GUIElement hotBarSelectionIndicator = ObjectLoader.loadGUIElement(GUIElement.getHotBarSelectionIndicatorVertices(), GUI_ELEMENT_TEXTURE_COORDINATES, new Vector2f(0, 0));
        hotBarSelectionIndicator.setTexture(new Texture(ObjectLoader.loadTexture("textures/HotBarSelectionIndicator.png")));
        player.setHotBarSelectionIndicator(hotBarSelectionIndicator);
        player.setSelectedHotBarSlot(0);

        generateInventoryElements(player.getInventoryElements(), Texture.ATLAS);
    }

    public static void reloadGUIElements(Player player) throws Exception {
        ArrayList<GUIElement> GUIElements = player.getGUIElements();
        GUIElement crossHair = GUIElements.removeFirst();
        ObjectLoader.removeVAO(crossHair.getVao());
        ObjectLoader.removeVBO(crossHair.getVbo1());
        ObjectLoader.removeVBO(crossHair.getVbo2());

        GUIElement hotBar = GUIElements.removeFirst();
        ObjectLoader.removeVAO(hotBar.getVao());
        ObjectLoader.removeVBO(hotBar.getVbo1());
        ObjectLoader.removeVBO(hotBar.getVbo2());

        GUIElement hotBarSelectionIndicator = player.getHotBarSelectionIndicator();
        ObjectLoader.removeVAO(hotBarSelectionIndicator.getVao());
        ObjectLoader.removeVBO(hotBarSelectionIndicator.getVbo1());
        ObjectLoader.removeVBO(hotBarSelectionIndicator.getVbo2());

        ArrayList<GUIElement> inventoryElements = player.getInventoryElements();
        for (GUIElement element : inventoryElements) {
            ObjectLoader.removeVAO(element.getVao());
            ObjectLoader.removeVBO(element.getVbo1());
            ObjectLoader.removeVBO(element.getVbo2());
        }
        inventoryElements.clear();
        player.setInventoryScroll(0.0f);

        loadGUIElements(player);
    }

    private final int vao, vertexCount;
    private final int vbo1, vbo2;
    private Texture texture;
    private Vector2f position;
}
