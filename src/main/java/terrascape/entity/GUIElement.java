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

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
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

    public static float[] getMaterialDisplayVertices(byte material) {
        if (material == 0) return new float[]{};
        WindowManager window = Launcher.getWindow();

        final int width = window.getWidth();
        final int height = window.getHeight();

        final float sin30 = (float) Math.sin(Math.toRadians(30)) * GUI_SIZE;
        final float cos30 = (float) Math.cos(Math.toRadians(30)) * GUI_SIZE;

        float[] vertices = new float[36];

        // Correct like this don't ask questions
        float widthX = 16;
        float widthY = 16 * GUI_SIZE;
        float widthZ = 16;

        float startX = 0;
        float startY = 0;

        float rightCornersX = startX + cos30 * widthX / width;
        float leftCornersX = startX - cos30 * widthZ / width;
        float backCornerX = rightCornersX + leftCornersX;

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

    public static float[] getMaterialDisplayTextureCoordinates(byte material) {
        if (material == AIR) return new float[]{};
        int textureIndex = Material.getTextureIndex(material);
        if (Material.isSemiTransparentMaterial(material)) textureIndex -= 16; // TODO less hacky solution
        float[] textureCoordinates = new float[36];

        final int textureFrontU = textureIndex & 15;
        final int textureFrontV = (textureIndex >> 4) & 15;

        final float upperU = (textureFrontU + 1) * 0.0625f;
        final float lowerU = (textureFrontU) * 0.0625f;
        final float upperV = (textureFrontV) * 0.0625f;
        final float lowerV = (textureFrontV + 1) * 0.0625f;
        textureCoordinates[0] = lowerU;
        textureCoordinates[1] = lowerV;
        textureCoordinates[2] = lowerU;
        textureCoordinates[3] = upperV;
        textureCoordinates[4] = upperU;
        textureCoordinates[5] = lowerV;
        textureCoordinates[6] = lowerU;
        textureCoordinates[7] = upperV;
        textureCoordinates[8] = upperU;
        textureCoordinates[9] = upperV;
        textureCoordinates[10] = upperU;
        textureCoordinates[11] = lowerV;

        textureCoordinates[12] = lowerU;
        textureCoordinates[13] = lowerV;
        textureCoordinates[14] = lowerU;
        textureCoordinates[15] = upperV;
        textureCoordinates[16] = upperU;
        textureCoordinates[17] = lowerV;
        textureCoordinates[18] = lowerU;
        textureCoordinates[19] = upperV;
        textureCoordinates[20] = upperU;
        textureCoordinates[21] = upperV;
        textureCoordinates[22] = upperU;
        textureCoordinates[23] = lowerV;

        textureCoordinates[24] = lowerU;
        textureCoordinates[25] = lowerV;
        textureCoordinates[26] = lowerU;
        textureCoordinates[27] = upperV;
        textureCoordinates[28] = upperU;
        textureCoordinates[29] = lowerV;
        textureCoordinates[30] = lowerU;
        textureCoordinates[31] = upperV;
        textureCoordinates[32] = upperU;
        textureCoordinates[33] = upperV;
        textureCoordinates[34] = upperU;
        textureCoordinates[35] = lowerV;

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

        if (x < 0.5f - ((1 << MATERIALS_PER_ROW_BITS) + 1) * 0.02f * GUI_SIZE) return 0;
        if (y > -0.5f + GUI_SIZE * 0.04f * (AMOUNT_OF_MATERIALS)) return 0;

        int valueX = (int) ((0.5 - 0.01 * GUI_SIZE - x) / (0.02 * GUI_SIZE));
        valueX = Math.min((1 << MATERIALS_PER_ROW_BITS) - 1, Math.max(valueX, 0));

        int valueY = (int) ((y - 0.005 * GUI_SIZE + 0.5) / (0.04 * GUI_SIZE));
        valueY = Math.min(AMOUNT_OF_MATERIALS - 1, valueY);

        byte hoveredOverMaterial = (byte) (valueY << MATERIALS_PER_ROW_BITS | valueX);
        if ((hoveredOverMaterial & 0xFF) >= AMOUNT_OF_MATERIALS) return AIR;
        return hoveredOverMaterial;
    }

    public static void generateInventoryElements(ArrayList<GUIElement> elements, Texture atlas) {
        for (int material = 1; material < AMOUNT_OF_MATERIALS; material++) {

            float[] vertices = GUIElement.getMaterialDisplayVertices((byte) material);

            float[] textureCoordinates = GUIElement.getMaterialDisplayTextureCoordinates((byte) material);
            float x = 0.5f - 0.02f * GUI_SIZE - 0.02f * GUI_SIZE * (material & (1 << MATERIALS_PER_ROW_BITS) - 1);
            float y = 0.5f - GUI_SIZE * 0.04f * (1 + (material >> MATERIALS_PER_ROW_BITS));
            GUIElement element = ObjectLoader.loadGUIElement(vertices, textureCoordinates, new Vector2f(x, y));
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
    private float size = 1.0f;
}
