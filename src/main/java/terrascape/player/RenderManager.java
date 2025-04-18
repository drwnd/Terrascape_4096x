package terrascape.player;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;
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

        loadMiscellaneous();
    }

    public void reloadShaders() {
        ShaderManager newOpaqueMaterialShader = null;
        ShaderManager newTransparentMaterialShader = null;
        ShaderManager newWaterMaterialShader = null;

        ShaderManager newSkyBoxShader = null;
        ShaderManager newGUIShader = null;
        ShaderManager newTextShader = null;

        ShaderManager newSSAOShader = null;
        ShaderManager newPostShader = null;

        ShaderManager newOpaqueParticleShader = null;
        ShaderManager newTransparentParticleShader = null;

        try {
            newOpaqueMaterialShader = createOpaqueMaterialShader();
            opaqueMaterialShader.cleanUp();
            opaqueMaterialShader = newOpaqueMaterialShader;
        } catch (Exception exception) {
            if (newOpaqueMaterialShader != null) newOpaqueMaterialShader.cleanUp();
            System.err.println("Failed to reload opaque material shader.");
            System.err.println(exception.getMessage());
        }
        try {
            newTransparentMaterialShader = createTransparentMaterialShader();
            transparentMaterialShader.cleanUp();
            transparentMaterialShader = newTransparentMaterialShader;
        } catch (Exception exception) {
            if (newTransparentMaterialShader != null) newTransparentMaterialShader.cleanUp();
            System.err.println("Failed to reload transparent material shader.");
            System.err.println(exception.getMessage());
        }
        try {
            newWaterMaterialShader = createWaterMaterialShader();
            waterMaterialShader.cleanUp();
            waterMaterialShader = newWaterMaterialShader;
        } catch (Exception exception) {
            if (newWaterMaterialShader != null) newWaterMaterialShader.cleanUp();
            System.err.println("Failed to reload water material Shader");
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

        try {
            newSSAOShader = createSSAOShader();
            ssaoShader.cleanUp();
            ssaoShader = newSSAOShader;
        } catch (Exception exception) {
            if (newSSAOShader != null) newSSAOShader.cleanUp();
            System.err.println("Failed to reload ssao shader.");
            System.err.println(exception.getMessage());
        }
        try {
            newPostShader = createPostShader();
            postShader.cleanUp();
            postShader = newPostShader;
        } catch (Exception exception) {
            if (newPostShader != null) newPostShader.cleanUp();
            System.err.println("Failed to reload post shader.");
            System.err.println(exception.getMessage());
        }

        try {
            newOpaqueParticleShader = createOpaqueParticleShader();
            opaqueParticleShader.cleanUp();
            opaqueParticleShader = newOpaqueParticleShader;
        } catch (Exception exception) {
            if (newOpaqueParticleShader != null) newOpaqueParticleShader.cleanUp();
            System.err.println("Failed to reload opaque particle shader.");
            System.err.println(exception.getMessage());
        }
        try {
            newTransparentParticleShader = createTransparentParticleShader();
            transparentParticleShader.cleanUp();
            transparentParticleShader = newTransparentParticleShader;
        } catch (Exception exception) {
            if (newTransparentParticleShader != null) newTransparentParticleShader.cleanUp();
            System.err.println("Failed to reload transparent particle shader.");
            System.err.println(exception.getMessage());
        }

        System.out.println("Shader reload completed.");
    }

    private void loadShaders() throws Exception {
        opaqueMaterialShader = createOpaqueMaterialShader();
        transparentMaterialShader = createTransparentMaterialShader();
        waterMaterialShader = createWaterMaterialShader();

        skyBoxShader = createSkyBoxShader();
        GUIShader = createGUIShader();
        textShader = createTextShader();

        ssaoShader = createSSAOShader();
        postShader = createPostShader();

        opaqueParticleShader = createOpaqueParticleShader();
        transparentParticleShader = createTransparentParticleShader();
    }

    private void createConstantBuffers() throws IllegalStateException {
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

        float[] noise = new float[4 * 4 * 3];
        for (int i = 0; i < noise.length; i += 3) {
            noise[i] = (float) (Math.random() * 2 - 1);
            noise[i + 1] = (float) (Math.random() * 2 - 1);
            noise[i + 2] = 0.0f;
        }

        modelIndexBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, modelIndexBuffer);
        IntBuffer buffer = Utils.storeDateInIntBuffer(indices);
        GL46.glBufferData(GL46.GL_ELEMENT_ARRAY_BUFFER, buffer, GL46.GL_STATIC_DRAW);

        modelVAO = ObjectLoader.loadModelVao();

        textRowVertexArray = ObjectLoader.loadTextRow();

        colorTexture = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, colorTexture);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGB, window.getWidth(), window.getHeight(), 0, GL46.GL_RGB, GL46.GL_UNSIGNED_BYTE, 0);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_LINEAR);

        depthTexture = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, depthTexture);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_DEPTH_COMPONENT, window.getWidth(), window.getHeight(), 0, GL46.GL_DEPTH_COMPONENT, GL46.GL_FLOAT, 0);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_LINEAR);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameterfv(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});

        noiseTexture = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, noiseTexture);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGB, 4, 4, 0, GL46.GL_RGB, GL46.GL_FLOAT, noise);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_REPEAT);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_REPEAT);

        ssaoTexture = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, ssaoTexture);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RED, window.getWidth(), window.getHeight(), 0, GL46.GL_RED, GL46.GL_FLOAT, 0);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);

        frameBuffer = GL46.glGenFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, frameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, colorTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Frame buffer not complete");
        }

        ssaoFrameBuffer = GL46.glGenFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, ssaoFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, ssaoTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("SSAO Frame buffer not complete");
    }

    private void loadTextures() throws Exception {
        atlas = new Texture(ObjectLoader.loadTexture("textures/atlas256.png"));
        textAtlas = new Texture(ObjectLoader.loadTexture("textures/textAtlas.png"));
    }

    private void loadMiscellaneous() throws Exception {
        screenOverlay = ObjectLoader.loadGUIElement(OVERLAY_VERTICES, GUI_ELEMENT_TEXTURE_COORDINATES, new Vector2f(0.0f, 0.0f));
        screenOverlay.setTexture(new Texture(ObjectLoader.loadTexture("textures/InventoryOverlay.png")));
    }

    private static ShaderManager createOpaqueMaterialShader() throws Exception {
        ShaderManager opaqueMaterialShader = new ShaderManager();
        opaqueMaterialShader.createVertexShader(ObjectLoader.loadResources("shaders/materialVertex.glsl"));
        opaqueMaterialShader.createFragmentShader(ObjectLoader.loadResources("shaders/opaqueMaterialFragment.glsl"));
        opaqueMaterialShader.link();
        opaqueMaterialShader.createUniform("textureSampler");
        opaqueMaterialShader.createUniform("projectionViewMatrix");
        opaqueMaterialShader.createUniform("worldPos");
        opaqueMaterialShader.createUniform("time");
        opaqueMaterialShader.createUniform("headUnderWater");
        opaqueMaterialShader.createUniform("cameraPosition");
        opaqueMaterialShader.createUniform("iCameraPosition");
        return opaqueMaterialShader;
    }

    private static ShaderManager createTransparentMaterialShader() throws Exception {
        ShaderManager transparentMaterialShader = new ShaderManager();
        transparentMaterialShader.createVertexShader(ObjectLoader.loadResources("shaders/materialVertex.glsl"));
        transparentMaterialShader.createFragmentShader(ObjectLoader.loadResources("shaders/transparentMaterialFragment.glsl"));
        transparentMaterialShader.link();
        transparentMaterialShader.createUniform("textureSampler");
        transparentMaterialShader.createUniform("projectionViewMatrix");
        transparentMaterialShader.createUniform("worldPos");
        transparentMaterialShader.createUniform("indexOffset");
        transparentMaterialShader.createUniform("iCameraPosition");
        return transparentMaterialShader;
    }

    private static ShaderManager createWaterMaterialShader() throws Exception {
        ShaderManager waterMaterialShader = new ShaderManager();
        waterMaterialShader.createVertexShader(ObjectLoader.loadResources("shaders/materialVertex.glsl"));
        waterMaterialShader.createFragmentShader(ObjectLoader.loadResources("shaders/waterMaterialFragment.glsl"));
        waterMaterialShader.link();
        waterMaterialShader.createUniform("textureSampler");
        waterMaterialShader.createUniform("projectionViewMatrix");
        waterMaterialShader.createUniform("worldPos");
        waterMaterialShader.createUniform("time");
        waterMaterialShader.createUniform("headUnderWater");
        waterMaterialShader.createUniform("cameraPosition");
        waterMaterialShader.createUniform("iCameraPosition");
        return waterMaterialShader;
    }

    private static ShaderManager createTextShader() throws Exception {
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

    private static ShaderManager createGUIShader() throws Exception {
        ShaderManager GUIShader = new ShaderManager();
        GUIShader.createVertexShader(ObjectLoader.loadResources("shaders/GUIVertex.glsl"));
        GUIShader.createFragmentShader(ObjectLoader.loadResources("shaders/GUIFragment.glsl"));
        GUIShader.link();
        GUIShader.createUniform("textureSampler");
        GUIShader.createUniform("position");
        return GUIShader;
    }

    private static ShaderManager createSkyBoxShader() throws Exception {
        ShaderManager skyBoxShader = new ShaderManager();
        skyBoxShader.createVertexShader(ObjectLoader.loadResources("shaders/skyBoxVertex.glsl"));
        skyBoxShader.createFragmentShader(ObjectLoader.loadResources("shaders/skyBoxFragment.glsl"));
        skyBoxShader.link();
        skyBoxShader.createUniform("textureSampler1");
        skyBoxShader.createUniform("textureSampler2");
        skyBoxShader.createUniform("projectionViewMatrix");
        skyBoxShader.createUniform("time");
        return skyBoxShader;
    }

    private static ShaderManager createSSAOShader() throws Exception {
        ShaderManager ssaoShader = new ShaderManager();
        ssaoShader.createVertexShader(ObjectLoader.loadResources("shaders/GUIVertex.glsl"));
        ssaoShader.createFragmentShader(ObjectLoader.loadResources("shaders/ssaoFragment.glsl"));
        ssaoShader.link();
        ssaoShader.createUniform("position");
        ssaoShader.createUniform("depthTexture");
        ssaoShader.createUniform("noiseTexture");
        ssaoShader.createUniform("projectionMatrix");
        ssaoShader.createUniform("projectionInverse");
        ssaoShader.createUniform("noiseScale");
        return ssaoShader;
    }

    private static ShaderManager createPostShader() throws Exception {
        ShaderManager newPostShader = new ShaderManager();
        newPostShader.createVertexShader(ObjectLoader.loadResources("shaders/GUIVertex.glsl"));
        newPostShader.createFragmentShader(ObjectLoader.loadResources("shaders/postFragment.glsl"));
        newPostShader.link();
        newPostShader.createUniform("position");
        newPostShader.createUniform("colorTexture");
        newPostShader.createUniform("ssaoTexture");
        newPostShader.createUniform("screenSize");
        return newPostShader;
    }

    private static ShaderManager createOpaqueParticleShader() throws Exception {
        ShaderManager opaqueParticleShader = new ShaderManager();
        opaqueParticleShader.createVertexShader(ObjectLoader.loadResources("shaders/particleVertex.glsl"));
        opaqueParticleShader.createFragmentShader(ObjectLoader.loadResources("shaders/opaqueMaterialFragment.glsl"));
        opaqueParticleShader.link();
        opaqueParticleShader.createUniform("projectionViewMatrix");
        opaqueParticleShader.createUniform("textureSampler");
        opaqueParticleShader.createUniform("headUnderWater");
        opaqueParticleShader.createUniform("time");
        opaqueParticleShader.createUniform("cameraPosition");
        opaqueParticleShader.createUniform("iCameraPosition");
        opaqueParticleShader.createUniform("currentTime");
        opaqueParticleShader.createUniform("indexOffset");

        return opaqueParticleShader;
    }

    private static ShaderManager createTransparentParticleShader() throws Exception {
        ShaderManager transparentParticleShader = new ShaderManager();
        transparentParticleShader.createVertexShader(ObjectLoader.loadResources("shaders/particleVertex.glsl"));
        transparentParticleShader.createFragmentShader(ObjectLoader.loadResources("shaders/transparentMaterialFragment.glsl"));
        transparentParticleShader.link();
        transparentParticleShader.createUniform("projectionViewMatrix");
        transparentParticleShader.createUniform("textureSampler");
        transparentParticleShader.createUniform("currentTime");
        transparentParticleShader.createUniform("indexOffset");
        transparentParticleShader.createUniform("iCameraPosition");

        return transparentParticleShader;
    }


    private void bindModel(OpaqueModel model) {
        GL46.glBindVertexArray(modelVAO);
        GL46.glEnableVertexAttribArray(0);

        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer);

        opaqueMaterialShader.setUniform("worldPos", model.X, model.Y, model.Z, 1 << model.LOD);
    }

    private void bindSkyBox(SkyBox skyBox) {
        GL46.glBindVertexArray(skyBox.getVao());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, skyBox.getTexture1().id());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, skyBox.getTexture2().id());
    }

    private void bindGUIElement(GUIElement element) {
        GL46.glBindVertexArray(element.getVao());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, element.getTexture().id());

        GUIShader.setUniform("textureSampler", 0);
        GUIShader.setUniform("position", element.getPosition());
    }

    private void bindTransparentModel(TransparentModel model, ShaderManager shader) {
        GL46.glBindVertexArray(modelVAO);
        GL46.glEnableVertexAttribArray(0);

        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer);

        shader.setUniform("worldPos", model.X, model.Y, model.Z, 1 << model.LOD);
    }

    private void unbind() {
        GL46.glDisableVertexAttribArray(0);
        GL46.glDisableVertexAttribArray(1);
        GL46.glBindVertexArray(0);
    }


    public void render(Camera camera, float passedTicks) {
        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera, window);
        Vector3f playerPosition = player.getCamera().getPosition();
        playerChunkX = Utils.floor(playerPosition.x) >> CHUNK_SIZE_BITS;
        playerChunkY = Utils.floor(playerPosition.y) >> CHUNK_SIZE_BITS;
        playerChunkZ = Utils.floor(playerPosition.z) >> CHUNK_SIZE_BITS;

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, frameBuffer);
        clear();
        if (xRay) GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_LINE);
        else GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_FILL);

        long start = System.nanoTime();
        renderSkyBox();
        if (player.printTimes) System.out.println("Skybox       " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderOpaqueGeometry(projectionViewMatrix, passedTicks);
        if (player.printTimes) System.out.println("opaque       " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderTransparentGeometry(projectionViewMatrix, passedTicks);
        if (player.printTimes) System.out.println("transparent  " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderParticles(projectionViewMatrix, passedTicks);
        if (player.printTimes) System.out.println("particles    " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderBufferToFrameWithPost();
        if (player.printTimes) System.out.println("ssao         " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderGUIElements();
        if (player.printTimes) System.out.println("GUI          " + (System.nanoTime() - start));

        start = System.nanoTime();
        if (player.isDebugScreenOpen()) renderDebugText();
        if (player.printTimes) System.out.println("debug        " + (System.nanoTime() - start));

        opaqueModels.clear();
        transparentModels.clear();
        GUIElements.clear();
        particles.clear();

        unbind();
    }

    private void renderSkyBox() {
        GL46.glDisable(GL46.GL_BLEND);

        skyBoxShader.bind();
        skyBoxShader.setUniform("textureSampler1", 0);
        skyBoxShader.setUniform("textureSampler2", 1);
        skyBoxShader.setUniform("time", time);
        skyBoxShader.setUniform("projectionViewMatrix", Transformation.createSkyBoxTransformationMatrix(player.getCamera(), window));

        bindSkyBox(skyBox);
        GL46.glDepthMask(false);

        GL46.glDrawElements(GL46.GL_TRIANGLES, skyBox.getVertexCount(), GL46.GL_UNSIGNED_INT, 0);

        GL46.glDepthMask(true);
        skyBoxShader.unBind();
    }

    private void renderOpaqueGeometry(Matrix4f projectionViewMatrix, float passedTicks) {
        opaqueMaterialShader.bind();
        opaqueMaterialShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        opaqueMaterialShader.setUniform("textureSampler", 0);
        opaqueMaterialShader.setUniform("time", getRenderTime(passedTicks));
        opaqueMaterialShader.setUniform("headUnderWater", headUnderWater ? 1 : 0);
        Vector3f cameraPosition = player.getCamera().getPosition();
        opaqueMaterialShader.setUniform("cameraPosition",
                Utils.fraction(cameraPosition.x / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.y / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.z / CHUNK_SIZE) * CHUNK_SIZE);
        opaqueMaterialShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, atlas.id());

        for (OpaqueModel model : opaqueModels) {
            if (!model.containGeometry) continue;
            int[] toRenderVertexCounts = model.getVertexCounts(playerChunkX, playerChunkY, playerChunkZ);

            bindModel(model);

            GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, model.getIndices(), toRenderVertexCounts);
        }

        opaqueMaterialShader.unBind();
    }

    private void renderTransparentGeometry(Matrix4f projectionViewMatrix, float passedTicks) {
        transparentMaterialShader.bind();
        transparentMaterialShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        transparentMaterialShader.setUniform("textureSampler", 0);
        Vector3f cameraPosition = player.getCamera().getPosition();
        transparentMaterialShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glDepthMask(false);

        GL46.glBlendFunc(GL46.GL_ZERO, GL46.GL_SRC_COLOR);
        for (TransparentModel model : transparentModels) {
            if (!model.containsGeometry) continue;
            transparentMaterialShader.setUniform("indexOffset", model.waterVertexCount >> 1);
            bindTransparentModel(model, transparentMaterialShader);

            GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.glassVertexCount * (6 / 2));
        }

        transparentMaterialShader.unBind();

        waterMaterialShader.bind();
        waterMaterialShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        waterMaterialShader.setUniform("textureSampler", 0);
        waterMaterialShader.setUniform("time", getRenderTime(passedTicks));
        waterMaterialShader.setUniform("headUnderWater", headUnderWater ? 1 : 0);
        waterMaterialShader.setUniform("cameraPosition",
                Utils.fraction(cameraPosition.x / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.y / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.z / CHUNK_SIZE) * CHUNK_SIZE);
        waterMaterialShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glDepthMask(true);
        for (TransparentModel model : transparentModels) {
            if (!model.containsGeometry) continue;
            bindTransparentModel(model, waterMaterialShader);

            GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.waterVertexCount * (6 / 2));
        }

        GL46.glDisable(GL46.GL_BLEND);
        waterMaterialShader.unBind();
    }

    private void renderParticles(Matrix4f projectionViewMatrix, float passedTicks) {
        if (opaqueParticleCount == 0 && transparentParticleCount == 0 && particles.isEmpty()) return;

        if (particlesHaveChanged) {
            particlesHaveChanged = false;
            resizeParticleBuffer();
            int index = 0;

            opaqueParticleCount = 0;
            transparentParticleCount = 0;
            for (Particle particle : particles) {
                if (Material.isSemiTransparentMaterial(particle.material())) continue;
                particlesData[index] = particle.x();
                particlesData[index + 1] = particle.y();
                particlesData[index + 2] = particle.z();
                particlesData[index + 3] = particle.packedVelocityGravity();
                particlesData[index + 4] = particle.packedLifeTimeRotationTexture();
                particlesData[index + 5] = particle.spawnTime();

                index += Particle.SHADER_PARTICLE_INT_SIZE;
                opaqueParticleCount++;
            }
            for (Particle particle : particles) {
                if (!Material.isSemiTransparentMaterial(particle.material())) continue;
                particlesData[index] = particle.x();
                particlesData[index + 1] = particle.y();
                particlesData[index + 2] = particle.z();
                particlesData[index + 3] = particle.packedVelocityGravity();
                particlesData[index + 4] = particle.packedLifeTimeRotationTexture();
                particlesData[index + 5] = particle.spawnTime();

                index += Particle.SHADER_PARTICLE_INT_SIZE;
                transparentParticleCount++;
            }

            GL46.glDeleteBuffers(particlesBuffer);
            particlesBuffer = GL46.glCreateBuffers();
            GL46.glNamedBufferData(particlesBuffer, particlesData, GL46.GL_STATIC_DRAW);
        }

        // Render opaque particles
        opaqueParticleShader.bind();
        opaqueParticleShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        opaqueParticleShader.setUniform("time", getRenderTime(passedTicks));
        opaqueParticleShader.setUniform("textureSampler", 0);
        opaqueParticleShader.setUniform("headUnderWater", headUnderWater ? 1 : 0);
        opaqueParticleShader.setUniform("currentTime", (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT));
        opaqueParticleShader.setUniform("indexOffset", 0);
        Vector3f cameraPosition = player.getCamera().getPosition();
        opaqueParticleShader.setUniform("cameraPosition",
                Utils.fraction(cameraPosition.x / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.y / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.z / CHUNK_SIZE) * CHUNK_SIZE);
        opaqueParticleShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, atlas.id());
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, particlesBuffer);
        GL46.glDisable(GL46.GL_BLEND);

        GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, opaqueParticleCount);

        opaqueParticleShader.unBind();

        // Render transparent particles
        transparentParticleShader.bind();
        transparentParticleShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        transparentParticleShader.setUniform("textureSampler", 0);
        transparentParticleShader.setUniform("currentTime", (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT));
        transparentParticleShader.setUniform("indexOffset", opaqueParticleCount);
        transparentParticleShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, atlas.id());
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, particlesBuffer);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_ZERO, GL46.GL_SRC_COLOR);
        GL46.glDepthMask(false);

        GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, transparentParticleCount);

        transparentParticleShader.unBind();
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glDepthMask(true);
    }

    private void renderBufferToFrameWithPost() {
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        Matrix4f projectionInverse = new Matrix4f();
        projectionMatrix.invert(projectionInverse);

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, ssaoFrameBuffer);
        clear();
        GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_FILL);
        ssaoShader.bind();
        GL46.glDisable(GL46.GL_DEPTH_TEST);

        GL46.glBindVertexArray(screenOverlay.getVao());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, depthTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, noiseTexture);

        ssaoShader.setUniform("depthTexture", 0);
        ssaoShader.setUniform("noiseTexture", 1);
        ssaoShader.setUniform("position", screenOverlay.getPosition());
        ssaoShader.setUniform("projectionMatrix", projectionMatrix);
        ssaoShader.setUniform("projectionInverse", projectionInverse);
        ssaoShader.setUniform("noiseScale", window.getWidth() >> 2, window.getHeight() >> 2);

        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, screenOverlay.getVertexCount());
        ssaoShader.unBind();

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0);
        clear();
        postShader.bind();

        GL46.glBindVertexArray(screenOverlay.getVao());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, colorTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, ssaoTexture);

        postShader.setUniform("colorTexture", 0);
        postShader.setUniform("ssaoTexture", 1);
        postShader.setUniform("position", screenOverlay.getPosition());
        postShader.setUniform("screenSize", window.getWidth(), window.getHeight());

        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, screenOverlay.getVertexCount());
        postShader.unBind();
    }

    private void renderGUIElements() {
        GUIShader.bind();
        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);

        // Elements with standard transparency
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);

        if (player.isInInventory()) {
            bindGUIElement(screenOverlay);

            GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, screenOverlay.getVertexCount());
        }

        for (GUIElement element : GUIElements) {
            bindGUIElement(element);

            GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, element.getVertexCount());
        }

        GUIShader.unBind();
        GL46.glDisable(GL46.GL_BLEND);

        if (!displayStrings.isEmpty()) {
            textShader.bind();
            GL46.glDisable(GL46.GL_DEPTH_TEST);
            GL46.glDisable(GL46.GL_CULL_FACE);
            GL46.glEnable(GL46.GL_BLEND);
            GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
            GL46.glBindTexture(GL46.GL_TEXTURE_2D, textAtlas.id());

            textShader.setUniform("screenSize", Launcher.getWindow().getWidth() >> 1, Launcher.getWindow().getHeight() >> 1);
            textShader.setUniform("charSize", TEXT_CHAR_SIZE_X, TEXT_CHAR_SIZE_Y);

            for (DisplayString string : displayStrings) renderDisplayString(string);
            displayStrings.clear();

            textShader.unBind();
        }
    }

    private void renderDebugText() {
        textShader.bind();
        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, textAtlas.id());

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
        double heightMapValue = Utils.floor(GenerationData.heightMapValue(x, z) * 1000) / 1000d;
        double erosionMapValue = Utils.floor(GenerationData.erosionMapValue(x, z) * 1000) / 1000d;
        double continentalMapValue = Utils.floor(GenerationData.continentalMapValue(x, z) * 1000) / 1000d;
        double riverMapValue = Utils.floor(GenerationData.riverMapValue(x, z) * 1000) / 1000d;
        double ridgeMapValue = Utils.floor(GenerationData.ridgeMapValue(x, y) * 1000) / 1000d;
        double temperatureMapValue = Utils.floor(GenerationData.temperatureMapValue(x, z) * 1000) / 1000d;
        double humidityMapValue = Utils.floor(GenerationData.humidityMapValue(x, z) * 1000) / 1000d;

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
        renderTextLine("Rendered opaque models:" + opaqueModels.size() + "/" + Chunk.countOpaqueModels(), Color.RED, ++line);
        renderTextLine("Rendered transparent models:" + transparentModels.size() + "/" + Chunk.countWaterModels(), Color.RED, ++line);
        renderTextLine("Rendered GUIElements:" + GUIElements.size(), Color.RED, ++line);
        renderTextLine("Rendered Particles:" + opaqueParticleCount, Color.RED, ++line);
        renderTextLine("Render distance XZ:" + RENDER_DISTANCE_XZ + " Render distance Y:" + RENDER_DISTANCE_Y, Color.ORANGE, ++line);
        renderTextLine("Concurrent played sounds:" + sourceCounter, Color.YELLOW, ++line);
        renderTextLine("Tick:" + EngineManager.getTick() + " Time:" + time, Color.WHITE, ++line);
        renderTextLine("To buffer chunks:" + ServerLogic.getAmountOfToBufferChunks(), Color.RED, ++line);
        renderTextLine("Hei:" + heightMapValue + " Ero:" + erosionMapValue + " Con:" + continentalMapValue, Color.GRAY, ++line);
        renderTextLine("Riv:" + riverMapValue + " Rid:" + ridgeMapValue, Color.GRAY, ++line);
        renderTextLine("Tem:" + temperatureMapValue + " Hum:" + humidityMapValue, Color.GRAY, ++line);
        renderTextLine("Resulting height: " + WorldGeneration.getResultingHeight(heightMapValue, erosionMapValue, continentalMapValue, riverMapValue, ridgeMapValue), Color.GRAY, ++line);

        GL46.glDisable(GL46.GL_BLEND);
        textShader.unBind();
    }

    private void renderTextLine(String text, Color color, int textLine) {
        textShader.setUniform("string", toIntFormat(text));
        textShader.setUniform("yOffset", textLine * TEXT_LINE_SPACING);
        textShader.setUniform("color", color);

        GL46.glBindVertexArray(textRowVertexArray);
        GL46.glEnableVertexAttribArray(0);
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, modelIndexBuffer);

        GL46.glDrawElements(GL46.GL_TRIANGLES, MAX_TEXT_LENGTH * 6, GL46.GL_UNSIGNED_INT, 0);
    }

    private void renderDisplayString(DisplayString string) {
        if (string.string() == null) return;
        textShader.setUniform("string", toIntFormat(string.string()));
        textShader.setUniform("yOffset", string.y());
        textShader.setUniform("xOffset", string.x());
        textShader.setUniform("color", Color.WHITE);

        GL46.glBindVertexArray(textRowVertexArray);
        GL46.glEnableVertexAttribArray(0);
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, modelIndexBuffer);

        GL46.glDrawElements(GL46.GL_TRIANGLES, 384, GL46.GL_UNSIGNED_INT, 0);
    }


    private void resizeParticleBuffer() {
        int requiredSize = particles.size() * Particle.SHADER_PARTICLE_INT_SIZE, length = particlesData.length;

        while (true) {
            if (requiredSize > length) length <<= 1;
            else if (requiredSize < length / 3) length >>= 1;
            else break;
        }

        if (length != particlesData.length) {
            int arrayLength = Math.max(1, length);
            particlesData = new int[arrayLength];
        }
    }

    private int[] toIntFormat(String text) {
        int[] array = new int[MAX_TEXT_LENGTH];

        byte[] stringBytes = text.getBytes(StandardCharsets.UTF_8);

        for (int index = 0, max = Math.min(text.length(), MAX_TEXT_LENGTH); index < max; index++) {
            array[index] = stringBytes[index];
        }
        return array;
    }

    private float getRenderTime(float passedTicks) {
        float renderTime = time + TIME_SPEED * passedTicks;
        if (renderTime > 1.0f) renderTime -= 2.0f;
        return renderTime;
    }


    public void processOpaqueModel(OpaqueModel model) {
        opaqueModels.add(model);
    }

    public void processWaterModel(TransparentModel transparentModel) {
        transparentModels.add(transparentModel);
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

    public void processParticle(Particle particle) {
        particles.add(particle);
    }


    public void setHeadUnderWater(boolean headUnderWater) {
        this.headUnderWater = headUnderWater;
    }

    private void clear() {
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT);
    }

    public void cleanUp() {
        opaqueMaterialShader.cleanUp();
        transparentMaterialShader.cleanUp();
        skyBoxShader.cleanUp();
        GUIShader.cleanUp();
        textShader.cleanUp();
        ssaoShader.cleanUp();
        postShader.cleanUp();
        opaqueParticleShader.cleanUp();
        transparentParticleShader.cleanUp();
    }

    public void setXRay(boolean xRay) {
        this.xRay = xRay;
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

    public void setParticlesHaveChanged() {
        particlesHaveChanged = true;
    }

    private final WindowManager window;
    private ShaderManager opaqueMaterialShader, transparentMaterialShader, waterMaterialShader;
    private ShaderManager opaqueParticleShader, transparentParticleShader;
    private ShaderManager skyBoxShader, GUIShader, textShader;
    private ShaderManager ssaoShader, postShader;

    private final ArrayList<OpaqueModel> opaqueModels = new ArrayList<>();
    private final ArrayList<TransparentModel> transparentModels = new ArrayList<>();
    private final ArrayList<GUIElement> GUIElements = new ArrayList<>();
    private final ArrayList<DisplayString> displayStrings = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Player player;
    private GUIElement screenOverlay;
    private SkyBox skyBox;
    private boolean headUnderWater = false;

    private float time = 1.0f;
    private int playerChunkX, playerChunkY, playerChunkZ;

    private int modelIndexBuffer;
    private int modelVAO;
    private int textRowVertexArray;
    private int frameBuffer, colorTexture, depthTexture, noiseTexture;
    private int ssaoFrameBuffer, ssaoTexture;
    private int[] particlesData = new int[1];
    private boolean particlesHaveChanged;
    private int particlesBuffer, opaqueParticleCount = 0, transparentParticleCount = 0;

    private Texture atlas;
    private Texture textAtlas;
    private boolean xRay;
}
