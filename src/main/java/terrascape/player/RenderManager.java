package terrascape.player;

import terrascape.server.*;
import terrascape.dataStorage.octree.Chunk;
import terrascape.entity.*;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

import terrascape.generation.GenerationData;
import terrascape.generation.WorldGeneration;
import terrascape.utils.Transformation;
import terrascape.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

import java.awt.Color;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class RenderManager {

    public RenderManager(Player player) {
        window = Launcher.getWindow();
        this.player = player;
    }

    public void init() throws Exception {

        loadTextures();

        loadShaders();

        createConstantBuffers();
    }

    public void reloadShaders() {
        ShaderManager newMaterialShader = null;
        ShaderManager newWaterShader = null;
        ShaderManager newSkyBoxShader = null;
        ShaderManager newGUIShader = null;
        ShaderManager newTextShader = null;

        try {
            newMaterialShader = createMaterialShader();
            materialShader.cleanUp();
            materialShader = newMaterialShader;
        } catch (Exception exception) {
            if (newMaterialShader != null) newMaterialShader.cleanUp();
            System.err.println("Failed to reload material shader.");
            System.err.println(exception.getMessage());
        }
        try {
            newWaterShader = createWaterShader();
            waterShader.cleanUp();
            waterShader = newWaterShader;
        } catch (Exception exception) {
            if (newWaterShader != null) newWaterShader.cleanUp();
            System.err.println("Failed to reload water shader.");
            System.err.println(exception.getMessage());
        }
        try {
            newSkyBoxShader = createSkyBoxShader();
            skyBoxShader.cleanUp();
            skyBoxShader = newSkyBoxShader;
        } catch (Exception exception) {
            if (newSkyBoxShader != null) newSkyBoxShader.cleanUp();
            System.err.println("Failed to reload sky box shader.");
            System.err.println(exception.getMessage());
        }
        try {
            newGUIShader = createGUIShader();
            GUIShader.cleanUp();
            GUIShader = newGUIShader;
        } catch (Exception exception) {
            if (newGUIShader != null) newGUIShader.cleanUp();
            System.err.println("Failed to reload GUI shader.");
            System.err.println(exception.getMessage());
        }
        try {
            newTextShader = createTextShader();
            textShader.cleanUp();
            textShader = newTextShader;
        } catch (Exception exception) {
            if (newTextShader != null) newTextShader.cleanUp();
            System.err.println("Failed to reload text shader.");
            System.err.println(exception.getMessage());
        }

        System.out.println("Shader reload completed.");
    }

    private void loadShaders() throws Exception {
        materialShader = createMaterialShader();
        waterShader = createWaterShader();
        skyBoxShader = createSkyBoxShader();
        GUIShader = createGUIShader();
        textShader = createTextShader();
    }

    private void createConstantBuffers() {
        int[] indices = new int[393216];
        int index = 0;
        for (int i = 0; i < indices.length; i += 6) {
            indices[i] = index;
            indices[i + 1] = index + 1;
            indices[i + 2] = index + 2;
            indices[i + 3] = index + 3;
            indices[i + 4] = index + 2;
            indices[i + 5] = index + 1;
            index += 4;
        }

        modelIndexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, modelIndexBuffer);
        IntBuffer buffer = Utils.storeDateInIntBuffer(indices);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        vao = ObjectLoader.loadVao();

        textRowVertexArray = ObjectLoader.loadTextRow();
    }

    private void loadTextures() throws Exception {
        atlas = new Texture(ObjectLoader.loadTexture("textures/atlas256.png"));
        textAtlas = new Texture(ObjectLoader.loadTexture("textures/textAtlas.png"));
    }

    private ShaderManager createTextShader() throws Exception {
        ShaderManager textShader = new ShaderManager();
        textShader.createVertexShader(ObjectLoader.loadResources("shaders/textVertex.glsl"));
        textShader.createFragmentShader(ObjectLoader.loadResources("shaders/textFragment.glsl"));
        textShader.link();
        textShader.createUniform("screenSize");
        textShader.createUniform("charSize");
        textShader.createUniform("string");
        textShader.createUniform("yOffset");
        textShader.createUniform("textureSampler");
        textShader.createUniform("xOffset");
        textShader.createUniform("color");
        return textShader;
    }

    private ShaderManager createGUIShader() throws Exception {
        ShaderManager GUIShader = new ShaderManager();
        GUIShader.createVertexShader(ObjectLoader.loadResources("shaders/GUIVertex.glsl"));
        GUIShader.createFragmentShader(ObjectLoader.loadResources("shaders/GUIFragment.glsl"));
        GUIShader.link();
        GUIShader.createUniform("textureSampler");
        GUIShader.createUniform("position");
        return GUIShader;
    }

    private ShaderManager createSkyBoxShader() throws Exception {
        ShaderManager skyBoxShader = new ShaderManager();
        skyBoxShader.createVertexShader(ObjectLoader.loadResources("shaders/skyBoxVertex.glsl"));
        skyBoxShader.createFragmentShader(ObjectLoader.loadResources("shaders/skyBoxFragment.glsl"));
        skyBoxShader.link();
        skyBoxShader.createUniform("textureSampler1");
        skyBoxShader.createUniform("textureSampler2");
        skyBoxShader.createUniform("projectionViewMatrix");
        skyBoxShader.createUniform("transformationMatrix");
        skyBoxShader.createUniform("time");
        return skyBoxShader;
    }

    private ShaderManager createWaterShader() throws Exception {
        ShaderManager waterShader = new ShaderManager();
        waterShader.createVertexShader(ObjectLoader.loadResources("shaders/materialVertex.glsl"));
        waterShader.createFragmentShader(ObjectLoader.loadResources("shaders/waterFragment.glsl"));
        waterShader.link();
        waterShader.createUniform("textureSampler");
        waterShader.createUniform("projectionViewMatrix");
        waterShader.createUniform("worldPos");
        waterShader.createUniform("time");
        waterShader.createUniform("headUnderWater");
        waterShader.createUniform("cameraPosition");
        return waterShader;
    }

    private ShaderManager createMaterialShader() throws Exception {
        ShaderManager materialShader = new ShaderManager();
        materialShader.createVertexShader(ObjectLoader.loadResources("shaders/materialVertex.glsl"));
        materialShader.createFragmentShader(ObjectLoader.loadResources("shaders/materialFragment.glsl"));
        materialShader.link();
        materialShader.createUniform("textureSampler");
        materialShader.createUniform("projectionViewMatrix");
        materialShader.createUniform("worldPos");
        materialShader.createUniform("time");
        materialShader.createUniform("headUnderWater");
        materialShader.createUniform("cameraPosition");
        return materialShader;
    }


    private void bindModel(OpaqueModel model) {
        GL30.glBindVertexArray(vao);
        GL20.glEnableVertexAttribArray(0);

        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer);

        materialShader.setUniform("worldPos", model.X, model.Y, model.Z, 1 << model.LOD);
    }

    private void bindSkyBox(SkyBox skyBox) {
        GL30.glBindVertexArray(skyBox.getVao());
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, skyBox.getTexture1().id());
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, skyBox.getTexture2().id());
    }

    private void bindGUIElement(GUIElement element) {
        GL30.glBindVertexArray(element.getVao());
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, element.getTexture().id());

        GUIShader.setUniform("textureSampler", 0);
        GUIShader.setUniform("position", element.getPosition());
    }

    private void bindWaterModel(WaterModel model) {
        GL30.glBindVertexArray(vao);
        GL20.glEnableVertexAttribArray(0);

        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer);

        materialShader.setUniform("worldPos", model.X, model.Y, model.Z, 1 << model.LOD);
    }

    private void unbind() {
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL30.glBindVertexArray(0);
    }

    public void render(Camera camera, float passedTicks) {
        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera, window);
        Vector3f playerPosition = player.getCamera().getPosition();
        playerChunkX = Utils.floor(playerPosition.x) >> CHUNK_SIZE_BITS;
        playerChunkY = Utils.floor(playerPosition.y) >> CHUNK_SIZE_BITS;
        playerChunkZ = Utils.floor(playerPosition.z) >> CHUNK_SIZE_BITS;

        clear();

        long start = System.nanoTime();
        renderSkyBox(projectionViewMatrix);
        if (player.printTimes) System.out.println("Skybox " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderOpaqueChunks(projectionViewMatrix, passedTicks);
        if (player.printTimes) System.out.println("opaque " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderWaterChunks(projectionViewMatrix, passedTicks);
        if (player.printTimes) System.out.println("water  " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderGUIElements();
        if (player.printTimes) System.out.println("GUI    " + (System.nanoTime() - start));

        start = System.nanoTime();
        if (player.isDebugScreenOpen()) renderDebugText();
        if (player.printTimes) System.out.println("debug  " + (System.nanoTime() - start));

        chunkModels.clear();
        foliageModels.clear();
        waterModels.clear();
        GUIElements.clear();

        unbind();
    }

    private void renderSkyBox(Matrix4f projectionViewMatrix) {
        skyBoxShader.bind();

        skyBoxShader.setUniform("textureSampler1", 0);
        skyBoxShader.setUniform("textureSampler2", 1);
        skyBoxShader.setUniform("time", time);
        skyBoxShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        skyBoxShader.setUniform("transformationMatrix", Transformation.createTransformationMatrix(skyBox.getPosition()));

        bindSkyBox(skyBox);

        GL11.glDrawElements(GL11.GL_TRIANGLES, skyBox.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);

        skyBoxShader.unBind();
    }

    private void renderOpaqueChunks(Matrix4f projectionViewMatrix, float passedTicks) {
        materialShader.bind();
        materialShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        materialShader.setUniform("textureSampler", 0);
        materialShader.setUniform("time", getRenderTime(passedTicks));
        materialShader.setUniform("headUnderWater", headUnderWater ? 1 : 0);
        materialShader.setUniform("cameraPosition", player.getCamera().getPosition());

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas.id());

        for (OpaqueModel model : chunkModels) {
            int[] toRenderVertexCounts = model.getVertexCounts(playerChunkX, playerChunkY, playerChunkZ);

            bindModel(model);

            GL14.glMultiDrawArrays(GL11.GL_TRIANGLES, model.getIndices(), toRenderVertexCounts);
        }

        materialShader.unBind();
    }

    private void renderWaterChunks(Matrix4f projectionViewMatrix, float passedTicks) {
        waterShader.bind();
        waterShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        waterShader.setUniform("textureSampler", 0);
        waterShader.setUniform("time", getRenderTime(passedTicks));
        waterShader.setUniform("headUnderWater", headUnderWater ? 1 : 0);
        waterShader.setUniform("cameraPosition", player.getCamera().getPosition());

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);

        for (int index = waterModels.size() - 1; index >= 0; index--) {
            WaterModel waterModel = waterModels.get(index);
            bindWaterModel(waterModel);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, waterModel.vertexCount);
        }

        GL11.glDisable(GL11.GL_BLEND);
        waterShader.unBind();
    }

    private float getRenderTime(float passedTicks) {
        float renderTime = time + TIME_SPEED * passedTicks;
        if (renderTime > 1.0f) renderTime -= 2.0f;
        return renderTime;
    }

    private void renderGUIElements() {
        GUIShader.bind();
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        if (player.isInInventory()) {
            GL11.glEnable(GL11.GL_BLEND);
            bindGUIElement(inventoryOverlay);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, inventoryOverlay.getVertexCount());

            GL11.glDisable(GL11.GL_BLEND);
        }

        for (GUIElement element : GUIElements) {
            bindGUIElement(element);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, element.getVertexCount());
        }
        GUIShader.unBind();

        if (!displayStrings.isEmpty()) {
            textShader.bind();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textAtlas.id());

            textShader.setUniform("screenSize", Launcher.getWindow().getWidth() >> 1, Launcher.getWindow().getHeight() >> 1);
            textShader.setUniform("charSize", TEXT_CHAR_SIZE_X, TEXT_CHAR_SIZE_Y);

            for (DisplayString string : displayStrings) renderDisplayString(string);
            displayStrings.clear();

            textShader.unBind();
        }
    }

    private void renderDebugText() {
        textShader.bind();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textAtlas.id());

        textShader.setUniform("screenSize", Launcher.getWindow().getWidth() >> 1, Launcher.getWindow().getHeight() >> 1);
        textShader.setUniform("charSize", TEXT_CHAR_SIZE_X, TEXT_CHAR_SIZE_Y);
        textShader.setUniform("xOffset", 0);

        int line = -1;
        final Vector3f position = player.getCamera().getPosition();
        final Vector3f direction = player.getCamera().getDirection();
        final Vector3f velocity = player.getMovement().getVelocity();

        Target target = Target.getTarget(position, direction);

        int x = Utils.floor(position.x), y = Utils.floor(position.y), z = Utils.floor(position.z);
        int chunkX = x >> CHUNK_SIZE_BITS, chunkY = y >> CHUNK_SIZE_BITS, chunkZ = z >> CHUNK_SIZE_BITS;
        int inChunkX = x & CHUNK_SIZE_MASK, inChunkY = y & CHUNK_SIZE_MASK, inChunkZ = z & CHUNK_SIZE_MASK;
        Chunk chunk = Chunk.getChunk(chunkX, chunkY, chunkZ, 0);
        int sourceCounter = 0;
        for (AudioSource source : Launcher.getSound().getSources()) if (source.isPlaying()) sourceCounter++;
        double heightMapValue = GenerationData.heightMapValue(x, z);
        double erosionMapValue = GenerationData.erosionMapValue(x, z);
        double continentalMapValue = GenerationData.continentalMapValue(x, z);
        double riverMapValue = GenerationData.riverMapValue(x, z);
        double ridgeMapValue = GenerationData.ridgeMapValue(x, y);
        double temperatureMapValue = GenerationData.temperatureMapValue(x, z);
        double humidityMapValue = GenerationData.humidityMapValue(x, z);

        renderTextLine("Frame rate:" + EngineManager.currentFrameRate + " last GT-time:" + Launcher.getServer().getLastGameTickProcessingTime() / 1_000_000 + "ms" + " current GT-time:" + Launcher.getServer().getDeltaTime() / 1_000_000 + "ms", Color.RED, ++line);
        renderTextLine("Memory:" + (Runtime.getRuntime().totalMemory() / 1_000_000) + "MB", Color.RED, ++line);
        renderTextLine("Coordinates: X:" + Utils.floor(position.x * 10) / 10f + " Y:" + Utils.floor(position.y * 10) / 10f + " Z:" + Utils.floor(position.z * 10) / 10f, Color.BLUE, ++line);
        renderTextLine("Chunk coordinates: X:" + chunkX + " Y:" + chunkY + " Z:" + chunkZ + " Id" + Utils.getChunkId(chunkX, chunkY, chunkZ), Color.BLUE, ++line);
        renderTextLine("In Chunk coordinates: X:" + inChunkX + " Y:" + inChunkY + " Z:" + inChunkZ, Color.BLUE, ++line);
        renderTextLine("Looking at: X:" + Utils.floor(direction.x * 100) / 100f + " Y:" + Utils.floor(direction.y * 100) / 100f + " Z:" + Utils.floor(direction.z * 100) / 100f, Color.CYAN, ++line);
        renderTextLine("Velocity: X:" + velocity.x + " Y:" + velocity.y + " Z:" + velocity.z, Color.CYAN, ++line);
        if (chunk != null) {
//            renderTextLine("OcclusionCullingData:" + Integer.toBinaryString(Chunk.getOcclusionCullingData(chunk.getIndex(), 0) & 0x7FFF) + " Damping:" + (Chunk.getOcclusionCullingDamper(Chunk.getOcclusionCullingData(chunk.getIndex(), 0)) == 0 ? "false" : "true"), Color.ORANGE, ++line);
            renderTextLine("Material in Head:" + Material.getMaterialName(Chunk.getMaterialInWorld(x, y, z)), Color.GREEN, ++line);
//            renderTextLine("Chunk byte size:" + chunk.getByteSize() + "B All chunks:" + Chunk.getAllChunksByteSize() / 1_000_000 + "MB", Color.RED, ++line); // ouch, performance
        }
        if (target != null) {
            renderTextLine("Looking at material: X:" + target.position().x + " Y:" + target.position().y + " Z:" + target.position().z, Color.GRAY, ++line);
            renderTextLine("Material:" + Material.getMaterialName(target.material()), Color.GRAY, ++line);
            renderTextLine("Intersected side:" + target.side(), Color.GRAY, ++line);
        }
        if (player.getMovement().isGrounded()) {
            renderTextLine("Standing on material:" + Material.getMaterialName(player.getMovement().getStandingMaterial()), Color.WHITE, ++line);
        }
        renderTextLine("Seed:" + SEED, Color.GREEN, ++line);
        renderTextLine("Rendered chunk models:" + chunkModels.size() + "/" + Chunk.countOpaqueModels(), Color.RED, ++line);
        renderTextLine("Rendered water models:" + waterModels.size() + "/" + Chunk.countWaterModels(), Color.RED, ++line);
        renderTextLine("Rendered foliage models:" + foliageModels.size(), Color.RED, ++line);
        renderTextLine("Rendered GUIElements:" + GUIElements.size(), Color.RED, ++line);
        renderTextLine("Render distance XZ:" + RENDER_DISTANCE_XZ + " Render distance Y:" + RENDER_DISTANCE_Y, Color.ORANGE, ++line);
        renderTextLine("Concurrent played sounds:" + sourceCounter, Color.YELLOW, ++line);
        renderTextLine("Tick:" + EngineManager.getTick() + " Time:" + time, Color.WHITE, ++line);
        renderTextLine("To buffer chunks:" + ServerLogic.getAmountOfToBufferChunks(), Color.RED, ++line);
        renderTextLine("Hei:" + Utils.floor(heightMapValue * 1000) / 1000d + " Ero:" + Utils.floor(erosionMapValue * 1000) / 1000d + " Con:" + Utils.floor(continentalMapValue * 1000) / 1000d, Color.GRAY, ++line);
        renderTextLine("Riv:" + Utils.floor(riverMapValue * 1000) / 1000d + " Rid:" + Utils.floor(ridgeMapValue * 1000) / 1000d, Color.GRAY, ++line);
        renderTextLine("Tem:" + Utils.floor(temperatureMapValue * 1000) / 1000d + " Hum:" + Utils.floor(humidityMapValue * 1000) / 1000d, Color.GRAY, ++line);
        renderTextLine("Resulting height: " + WorldGeneration.getResultingHeight(heightMapValue, erosionMapValue, continentalMapValue, riverMapValue, ridgeMapValue), Color.GRAY, ++line);

        textShader.unBind();
    }

    private void renderTextLine(String text, Color color, int textLine) {
        textShader.setUniform("string", toIntFormat(text));
        textShader.setUniform("yOffset", textLine * TEXT_LINE_SPACING);
        textShader.setUniform("color", color);

        GL30.glBindVertexArray(textRowVertexArray);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, modelIndexBuffer);

        GL11.glDrawElements(GL11.GL_TRIANGLES, MAX_TEXT_LENGTH * 6, GL11.GL_UNSIGNED_INT, 0);
    }

    private void renderDisplayString(DisplayString string) {
        if (string.string() == null) return;
        textShader.setUniform("string", toIntFormat(string.string()));
        textShader.setUniform("yOffset", string.y());
        textShader.setUniform("xOffset", string.x());
        textShader.setUniform("color", Color.WHITE);

        GL30.glBindVertexArray(textRowVertexArray);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, modelIndexBuffer);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 384, GL11.GL_UNSIGNED_INT, 0);
    }

    private int[] toIntFormat(String text) {
        int[] array = new int[MAX_TEXT_LENGTH];

        byte[] stringBytes = text.getBytes(StandardCharsets.UTF_8);

        for (int index = 0, max = Math.min(text.length(), MAX_TEXT_LENGTH); index < max; index++) {
            array[index] = stringBytes[index];
        }
        return array;
    }

    public void processOpaqueModel(OpaqueModel model) {
        chunkModels.add(model);
    }

    public void processWaterModel(WaterModel waterModel) {
        waterModels.add(waterModel);
    }

    public void processSkyBox(SkyBox skyBox) {
        this.skyBox = skyBox;
    }

    public void processGUIElement(GUIElement element) {
        GUIElements.add(element);
    }

    public void processDisplayString(DisplayString string) {
        displayStrings.add(string);
    }

    public void setHeadUnderWater(boolean headUnderWater) {
        this.headUnderWater = headUnderWater;
    }

    public void setInventoryOverlay(GUIElement inventoryOverlay) {
        this.inventoryOverlay = inventoryOverlay;
    }

    private void clear() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void cleanUp() {
        materialShader.cleanUp();
        skyBoxShader.cleanUp();
        GUIShader.cleanUp();
    }

    public void setXRay(boolean xRay) {
        this.xRay = xRay;
        if (xRay) GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        else GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }

    public boolean isxRay() {
        return xRay;
    }

    public void incrementTime() {
        time += TIME_SPEED;
        if (time > 1.0f) time -= 2.0f;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    private final WindowManager window;
    private ShaderManager materialShader, waterShader, skyBoxShader, GUIShader, textShader;

    private final ArrayList<OpaqueModel> chunkModels = new ArrayList<>();
    private final ArrayList<OpaqueModel> foliageModels = new ArrayList<>();
    private final ArrayList<WaterModel> waterModels = new ArrayList<>();
    private final ArrayList<GUIElement> GUIElements = new ArrayList<>();
    private final ArrayList<DisplayString> displayStrings = new ArrayList<>();
    private final Player player;
    private GUIElement inventoryOverlay;
    private SkyBox skyBox;
    private boolean headUnderWater = false;

    private float time = 1.0f;
    private int playerChunkX, playerChunkY, playerChunkZ;

    private int modelIndexBuffer;
    private int vao;
    private int textRowVertexArray;

    private Texture atlas;
    private Texture textAtlas;
    private boolean xRay;
}
