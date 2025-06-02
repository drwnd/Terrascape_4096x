package terrascape.player;

import org.joml.*;
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

import java.awt.Color;
import java.lang.Math;
import java.lang.Runtime;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class RenderManager {

    public RenderManager(Player player) {
        window = Launcher.getWindow();
        this.player = player;
    }

    public void init() throws Exception {
        loadShaders();

        createConstantBuffers();

        loadMiscellaneous();
    }

    public void reloadShaders() {
        opaqueMaterialShader.reload();
        transparentMaterialShader.reload();
        waterMaterialShader.reload();

        skyBoxShader.reload();
        GUIShader.reload();
        textShader.reload();
        copyDepthShader.reload();

        ssaoShader.reload();
        postShader.reload();
        lightShader.reload();
        lightPrePassShader.reload();

        opaqueParticleShader.reload();
        transparentParticleShader.reload();

        System.out.println("Shader reload completed.");
    }

    private void loadShaders() throws Exception {
        opaqueMaterialShader = ShaderManager.createOpaqueMaterialShader();
        transparentMaterialShader = ShaderManager.createTransparentMaterialShader();
        waterMaterialShader = ShaderManager.createWaterMaterialShader();

        skyBoxShader = ShaderManager.createSkyBoxShader();
        GUIShader = ShaderManager.createGUIShader();
        textShader = ShaderManager.createTextShader();
        copyDepthShader = ShaderManager.createCopyDepthShader();

        ssaoShader = ShaderManager.createSSAOShader();
        postShader = ShaderManager.createPostShader();
        lightShader = ShaderManager.createLightShader();
        lightPrePassShader = ShaderManager.createLightPrePassShader();

        opaqueParticleShader = ShaderManager.createOpaqueParticleShader();
        transparentParticleShader = ShaderManager.createTransparentParticleShader();
    }

    private void createConstantBuffers() throws IllegalStateException {
        float[] noise = new float[4 * 4 * 3];
        for (int i = 0; i < noise.length; i += 3) {
            noise[i] = (float) (Math.random() * 2 - 1);
            noise[i + 1] = (float) (Math.random() * 2 - 1);
            noise[i + 2] = 0.0f;
        }

        textRowVertexArray = ObjectLoader.loadTextRow();
        modelIndexBuffer = ObjectLoader.loadModelIndexBuffer();

        normalTexture = ObjectLoader.create2DTexture(GL46.GL_RGB16F, GL46.GL_RGB, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_FLOAT);
        positionTexture = ObjectLoader.create2DTexture(GL46.GL_RGB16F, GL46.GL_RGB, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_FLOAT);
        colorTexture = ObjectLoader.create2DTexture(GL46.GL_RGBA, GL46.GL_RGBA, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_UNSIGNED_BYTE);
        propertiesTexture = ObjectLoader.create2DTexture(GL46.GL_RED, GL46.GL_RED, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_UNSIGNED_BYTE);
        lightTexture = ObjectLoader.create2DTexture(GL46.GL_RGB, GL46.GL_RGB, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_UNSIGNED_BYTE);
        int finalColorTexture = ObjectLoader.create2DTexture(GL46.GL_RGB, GL46.GL_RGB, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_UNSIGNED_BYTE);

        depthTexture = ObjectLoader.create2DTexture(GL46.GL_DEPTH32F_STENCIL8, GL46.GL_DEPTH_STENCIL, window.getWidth(), window.getHeight(), GL46.GL_LINEAR, GL46.GL_FLOAT_32_UNSIGNED_INT_24_8_REV);
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

        ssaoTexture = ObjectLoader.create2DTexture(GL46.GL_RED, GL46.GL_RED, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_FLOAT);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);

        deferredRenderingFrameBuffer = GL46.glGenFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, deferredRenderingFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, normalTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT1, GL46.GL_TEXTURE_2D, positionTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT2, GL46.GL_TEXTURE_2D, colorTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT3, GL46.GL_TEXTURE_2D, propertiesTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        GL46.glDrawBuffers(new int[]{GL46.GL_COLOR_ATTACHMENT0, GL46.GL_COLOR_ATTACHMENT1, GL46.GL_COLOR_ATTACHMENT2, GL46.GL_COLOR_ATTACHMENT3});
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete");

        ssaoFrameBuffer = GL46.glGenFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, ssaoFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, ssaoTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("SSAO Frame buffer not complete");

        lightsFrameBuffer = GL46.glGenFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, lightsFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, lightTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Light Frame buffer not complete");

        finalFrameBuffer = GL46.glGenFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, finalFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, finalColorTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Final Frame buffer not complete");
    }

    private void loadMiscellaneous() throws Exception {
        screenOverlay = ObjectLoader.loadGUIElement(OVERLAY_VERTICES, GUI_ELEMENT_TEXTURE_COORDINATES, new Vector2f(0.0f, 0.0f));
        screenOverlay.setTexture(new Texture(ObjectLoader.loadTexture("textures/InventoryOverlay.png")));
        sphere = ObjectLoader.loadUnitSphere(15, 15);
        skyBox = ObjectLoader.loadSkyBox(player.getCamera().getPosition());
    }


    private void bindModel(OpaqueModel model) {
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

        GUIShader.setUniform("textureAtlas", 0);
        GUIShader.setUniform("position", element.getPosition());
    }

    private void bindTransparentModel(TransparentModel model, ShaderManager shader) {
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
        Vector3f sunDirection = Transformation.getSunDirection(getRenderTime(passedTicks));

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, deferredRenderingFrameBuffer);
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT);
        if (xRay) GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_LINE);
        else GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_FILL);

        long start = System.nanoTime();
        renderOpaqueGeometry(projectionViewMatrix);
        if (player.printTimes) System.out.println("opaque geometry       " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderOpaqueParticles(projectionViewMatrix);
        if (player.printTimes) System.out.println("opaque particles      " + (System.nanoTime() - start));

        start = System.nanoTime();
        computeSSAO();
        if (player.printTimes) System.out.println("compute ssao          " + (System.nanoTime() - start));

        start = System.nanoTime();
        computeLights(projectionViewMatrix);
        if (player.printTimes) System.out.println("compute lights        " + (System.nanoTime() - start));

        start = System.nanoTime();
        doDeferredCalculation(sunDirection, passedTicks);
        if (player.printTimes) System.out.println("ssao and opaque light " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderSkyBox();
        if (player.printTimes) System.out.println("Skybox                " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderTransparentGeometry(projectionViewMatrix);
        if (player.printTimes) System.out.println("transparent geometry  " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderTransparentParticles(projectionViewMatrix);
        if (player.printTimes) System.out.println("transparent particles " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderWater(projectionViewMatrix, sunDirection, passedTicks);
        if (player.printTimes) System.out.println("water                 " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderGUIElements();
        if (player.printTimes) System.out.println("GUI                   " + (System.nanoTime() - start));

        start = System.nanoTime();
        if (player.isDebugScreenOpen()) renderDebugText();
        if (player.printTimes) System.out.println("debug                 " + (System.nanoTime() - start));

        start = System.nanoTime();
        renderToScreen();
        if (player.printTimes) System.out.println("copy to screen        " + (System.nanoTime() - start));

        opaqueModels.clear();
        transparentModels.clear();
        lightModels.clear();
        GUIElements.clear();
        particles.clear();

        unbind();
    }

    private void renderSkyBox() {
        GL46.glDisable(GL46.GL_BLEND);

        skyBoxShader.bind();
        skyBoxShader.setUniform("textureAtlas1", 0);
        skyBoxShader.setUniform("textureAtlas2", 1);
        skyBoxShader.setUniform("time", time);
        skyBoxShader.setUniform("projectionViewMatrix", Transformation.createSkyBoxTransformationMatrix(player.getCamera(), window));

        bindSkyBox(skyBox);
        GL46.glDepthMask(false);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);

        GL46.glDrawElements(GL46.GL_TRIANGLES, skyBox.getVertexCount(), GL46.GL_UNSIGNED_INT, 0);

        GL46.glDepthMask(true);
    }

    private void renderOpaqueGeometry(Matrix4f projectionViewMatrix) {
        opaqueMaterialShader.bind();
        opaqueMaterialShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        Vector3f cameraPosition = player.getCamera().getPosition();
        opaqueMaterialShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);
        opaqueMaterialShader.setUniform("textureAtlas", 0);
        opaqueMaterialShader.setUniform("propertiesTexture", 1);

        Vector3f playerPosition = player.getCamera().getPosition();
        int playerChunkX = Utils.floor(playerPosition.x) >> CHUNK_SIZE_BITS;
        int playerChunkY = Utils.floor(playerPosition.y) >> CHUNK_SIZE_BITS;
        int playerChunkZ = Utils.floor(playerPosition.z) >> CHUNK_SIZE_BITS;

        GL46.glBindVertexArray(skyBox.getVao()); // Just bind something IDK
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.ATLAS.id());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.PROPERTIES_ATLAS.id());

        for (OpaqueModel model : opaqueModels) {
            if (!model.containGeometry) continue;
            int[] toRenderVertexCounts = model.getVertexCounts(playerChunkX, playerChunkY, playerChunkZ);

            bindModel(model);

            GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, model.getIndices(), toRenderVertexCounts);
        }
    }

    private void renderOpaqueParticles(Matrix4f projectionViewMatrix) {
        if (opaqueParticleCount == 0 && transparentParticleCount == 0 && particles.isEmpty()) return;

        if (particlesHaveChanged) {
            particlesHaveChanged = false;
            resizeParticleBuffer();
            opaqueParticleCount = 0;
            transparentParticleCount = 0;

            int index = addParticlesToParticlesData(0, true);
            addParticlesToParticlesData(index, false);

            GL46.glDeleteBuffers(particlesBuffer);
            particlesBuffer = GL46.glCreateBuffers();
            GL46.glNamedBufferData(particlesBuffer, particlesData, GL46.GL_STATIC_DRAW);
        }

        opaqueParticleShader.bind();
        opaqueParticleShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        Vector3f cameraPosition = player.getCamera().getPosition();
        opaqueParticleShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);
        opaqueParticleShader.setUniform("textureAtlas", 0);
        opaqueParticleShader.setUniform("propertiesTexture", 1);
        opaqueParticleShader.setUniform("indexOffset", 0);
        opaqueParticleShader.setUniform("currentTime", (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT));

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.ATLAS.id());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.PROPERTIES_ATLAS.id());
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, particlesBuffer);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_CULL_FACE);

        GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, opaqueParticleCount);
    }

    private void computeSSAO() {
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        Matrix4f projectionInverse = new Matrix4f();
        projectionMatrix.invert(projectionInverse);

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, ssaoFrameBuffer);
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT);
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
    }

    private void computeLights(Matrix4f projectionViewMatrix) {
        lightPrePassShader.bind();
        lightPrePassShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        Vector3f cameraPosition = player.getCamera().getPosition();
        lightPrePassShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, lightsFrameBuffer);
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_STENCIL_BUFFER_BIT);

        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glDepthMask(false);
        GL46.glEnable(GL46.GL_STENCIL_TEST);
        GL46.glStencilFunc(GL46.GL_ALWAYS, 0, 0);
        GL46.glStencilOpSeparate(GL46.GL_BACK, GL46.GL_KEEP, GL46.GL_INCR_WRAP, GL46.GL_KEEP);
        GL46.glStencilOpSeparate(GL46.GL_FRONT, GL46.GL_KEEP, GL46.GL_DECR_WRAP, GL46.GL_KEEP);

        renderLights(lightPrePassShader);

        GL46.glDisable(GL46.GL_DEPTH_TEST);

        lightShader.bind();
        lightShader.setUniform("screenSize", window.getWidth(), window.getHeight());
        lightShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);
        lightShader.setUniform("projectionViewMatrix", projectionViewMatrix);

        lightShader.setUniform("normalTexture", 0);
        lightShader.setUniform("positionTexture", 1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, normalTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, positionTexture);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_ONE, GL46.GL_ONE);

        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glCullFace(GL46.GL_FRONT);
        GL46.glStencilFunc(GL46.GL_NOTEQUAL, 0, 0xFF);

        renderLights(lightShader);

        GL46.glDisable(GL46.GL_STENCIL_TEST);
        GL46.glCullFace(GL46.GL_BACK);
    }

    private void doDeferredCalculation(Vector3f sunDirection, float passedTicks) {
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, finalFrameBuffer);
        postShader.bind();

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, normalTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, positionTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE2);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, colorTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE3);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, ssaoTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE4);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, lightTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE5);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, propertiesTexture);

        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glDepthMask(false);
        GL46.glColorMask(true, true, true, true);

        postShader.setUniform("time", getRenderTime(passedTicks));
        postShader.setUniform("flags", headUnderWater ? 1 : 0);
        postShader.setUniform("sunDirection", sunDirection);
        Vector3f cameraPosition = player.getCamera().getPosition();
        postShader.setUniform("cameraPosition",
                Utils.fraction(cameraPosition.x / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.y / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.z / CHUNK_SIZE) * CHUNK_SIZE);

        postShader.setUniform("normalTexture", 0);
        postShader.setUniform("positionTexture", 1);
        postShader.setUniform("colorTexture", 2);
        postShader.setUniform("ssaoTexture", 3);
        postShader.setUniform("lightTexture", 4);
        postShader.setUniform("propertiesTexture", 5);
        postShader.setUniform("position", screenOverlay.getPosition());
        postShader.setUniform("screenSize", window.getWidth(), window.getHeight());

        GL46.glBindVertexArray(screenOverlay.getVao());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);
        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, screenOverlay.getVertexCount());

        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDepthMask(true);
    }

    private void renderTransparentGeometry(Matrix4f projectionViewMatrix) {
        transparentMaterialShader.bind();
        transparentMaterialShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        transparentMaterialShader.setUniform("textureAtlas", 0);
        Vector3f cameraPosition = player.getCamera().getPosition();
        transparentMaterialShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.ATLAS.id());
        GL46.glDepthMask(false);

        GL46.glBlendFunc(GL46.GL_ZERO, GL46.GL_SRC_COLOR);
        for (TransparentModel model : transparentModels) {
            if (!model.containsGeometry) continue;
            transparentMaterialShader.setUniform("indexOffset", model.waterVertexCount >> 1);
            bindTransparentModel(model, transparentMaterialShader);

            GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.glassVertexCount * (6 / 2));
        }
    }

    private void renderTransparentParticles(Matrix4f projectionViewMatrix) {
        if (opaqueParticleCount == 0 && transparentParticleCount == 0 && particles.isEmpty()) return;

        transparentParticleShader.bind();
        transparentParticleShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        transparentParticleShader.setUniform("currentTime", (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT));
        transparentParticleShader.setUniform("indexOffset", opaqueParticleCount);
        Vector3f cameraPosition = player.getCamera().getPosition();
        transparentParticleShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);
        transparentParticleShader.setUniform("textureAtlas", 0);

        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDepthMask(false);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.ATLAS.id());

        GL46.glBlendFunc(GL46.GL_ZERO, GL46.GL_SRC_COLOR);

        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, particlesBuffer);
        GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, transparentParticleCount);

        GL46.glDepthMask(true);
    }

    private void renderWater(Matrix4f projectionViewMatrix, Vector3f sunDirection, float passedTicks) {
        waterMaterialShader.bind();
        waterMaterialShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        waterMaterialShader.setUniform("textureAtlas", 0);
        waterMaterialShader.setUniform("time", getRenderTime(passedTicks));
        waterMaterialShader.setUniform("flags", headUnderWater ? 1 : 0);
        waterMaterialShader.setUniform("sunDirection", sunDirection);
        Vector3f cameraPosition = player.getCamera().getPosition();
        waterMaterialShader.setUniform("cameraPosition",
                Utils.fraction(cameraPosition.x / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.y / CHUNK_SIZE) * CHUNK_SIZE,
                Utils.fraction(cameraPosition.z / CHUNK_SIZE) * CHUNK_SIZE);
        waterMaterialShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glDepthMask(true);
        for (TransparentModel model : transparentModels) {
            if (!model.containsGeometry) continue;
            bindTransparentModel(model, waterMaterialShader);

            GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.waterVertexCount * (6 / 2));
        }

        GL46.glDisable(GL46.GL_BLEND);
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

        GL46.glDisable(GL46.GL_BLEND);

        if (!displayStrings.isEmpty()) {
            textShader.bind();
            GL46.glDisable(GL46.GL_DEPTH_TEST);
            GL46.glDisable(GL46.GL_CULL_FACE);
            GL46.glEnable(GL46.GL_BLEND);
            GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
            GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.TEXT_ATLAS.id());

            textShader.setUniform("screenSize", Launcher.getWindow().getWidth() >> 1, Launcher.getWindow().getHeight() >> 1);
            textShader.setUniform("charSize", TEXT_CHAR_SIZE_X, TEXT_CHAR_SIZE_Y);

            for (DisplayString string : displayStrings) renderDisplayString(string);
            displayStrings.clear();
        }
    }

    private void renderDebugText() {
        textShader.bind();
        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.TEXT_ATLAS.id());

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
        int sourceCounter = Launcher.getSound().countActiveSources();
        double heightMapValue = Utils.floor(GenerationData.heightMapValue(x, z) * 1000) / 1000d;
        double erosionMapValue = Utils.floor(GenerationData.erosionMapValue(x, z) * 1000) / 1000d;
        double continentalMapValue = Utils.floor(GenerationData.continentalMapValue(x, z) * 1000) / 1000d;
        double riverMapValue = Utils.floor(GenerationData.riverMapValue(x, z) * 1000) / 1000d;
        double ridgeMapValue = Utils.floor(GenerationData.ridgeMapValue(x, y) * 1000) / 1000d;
        double temperatureMapValue = Utils.floor(GenerationData.temperatureMapValue(x, z) * 1000) / 1000d;
        double humidityMapValue = Utils.floor(GenerationData.humidityMapValue(x, z) * 1000) / 1000d;
        int resultingHeight = WorldGeneration.getResultingHeight(heightMapValue, erosionMapValue, continentalMapValue, riverMapValue, ridgeMapValue);

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
        renderTextLine("Rendered light models:" + lightModels.size() + "/" + Chunk.countLightModels(), Color.RED, ++line);
        renderTextLine("Rendered GUIElements:" + GUIElements.size(), Color.RED, ++line);
        renderTextLine("Rendered Particles:" + (opaqueParticleCount + transparentParticleCount), Color.RED, ++line);
        renderTextLine("Render distance XZ:" + RENDER_DISTANCE_XZ + " Render distance Y:" + RENDER_DISTANCE_Y, Color.ORANGE, ++line);
        renderTextLine("Concurrent played sounds:" + sourceCounter, Color.YELLOW, ++line);
        renderTextLine("Tick:" + EngineManager.getTick() + " Time:" + time, Color.WHITE, ++line);
        renderTextLine("To buffer chunks:" + ServerLogic.getAmountOfToBufferChunks(), Color.RED, ++line);
        renderTextLine("Hei:" + heightMapValue + " Ero:" + erosionMapValue + " Con:" + continentalMapValue, Color.GRAY, ++line);
        renderTextLine("Riv:" + riverMapValue + " Rid:" + ridgeMapValue, Color.GRAY, ++line);
        renderTextLine("Tem:" + temperatureMapValue + " Hum:" + humidityMapValue, Color.GRAY, ++line);
        renderTextLine("Resulting height:" + WorldGeneration.getResultingHeight(heightMapValue, erosionMapValue, continentalMapValue, riverMapValue, ridgeMapValue), Color.GRAY, ++line);
        renderTextLine("Biome: " + WorldGeneration.getBiome(temperatureMapValue, humidityMapValue, 96, resultingHeight, erosionMapValue, continentalMapValue).getName(), Color.GRAY, ++line);

        GL46.glDisable(GL46.GL_BLEND);
    }

    private void renderToScreen() {
        GL46.glBindFramebuffer(GL46.GL_READ_FRAMEBUFFER, finalFrameBuffer);
        GL46.glBindFramebuffer(GL46.GL_DRAW_FRAMEBUFFER, 0);
        GL46.glBlitFramebuffer(0, 0, window.getWidth(), window.getHeight(), 0, 0, window.getWidth(), window.getHeight(), GL46.GL_COLOR_BUFFER_BIT, GL46.GL_NEAREST);
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

    private void renderLights(ShaderManager lightShader) {
        for (LightModel lightModel : lightModels) {
            lightShader.setUniform("worldPos", lightModel.x(), lightModel.y(), lightModel.z(), 1 << lightModel.lod());
            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, lightModel.buffer());
            GL46.glBindVertexArray(sphere.vao());
            GL46.glEnableVertexAttribArray(0);
            GL46.glDrawElementsInstanced(GL46.GL_TRIANGLES, sphere.vertexCount(), GL46.GL_UNSIGNED_INT, 0, lightModel.count());
        }
    }


    private int addParticlesToParticlesData(int index, boolean isOpaque) {
        for (Particle particle : particles) {
            if (Material.isSemiTransparentMaterial(particle.material()) == isOpaque) continue;
            particlesData[index] = particle.x();
            particlesData[index + 1] = particle.y();
            particlesData[index + 2] = particle.z();
            particlesData[index + 3] = particle.packedVelocityGravity();
            particlesData[index + 4] = particle.packedLifeTimeRotationTexture();
            particlesData[index + 5] = particle.spawnTime();

            index += Particle.SHADER_PARTICLE_INT_SIZE;
            if (isOpaque) opaqueParticleCount++;
            else transparentParticleCount++;
        }
        return index;
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


    public void processOpaqueModel(OpaqueModel opaqueModel) {
        opaqueModels.add(opaqueModel);
    }

    public void processLightModel(LightModel lightModel) {
        if (lightModel == null) return;
        lightModels.add(lightModel);
    }

    public void processWaterModel(TransparentModel transparentModel) {
        transparentModels.add(transparentModel);
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
    private ShaderManager skyBoxShader, GUIShader, textShader, copyDepthShader;
    private ShaderManager ssaoShader, postShader, lightShader, lightPrePassShader;

    private final ArrayList<OpaqueModel> opaqueModels = new ArrayList<>();
    private final ArrayList<TransparentModel> transparentModels = new ArrayList<>();
    private final ArrayList<LightModel> lightModels = new ArrayList<>();
    private final ArrayList<GUIElement> GUIElements = new ArrayList<>();
    private final ArrayList<DisplayString> displayStrings = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Player player;
    private GUIElement screenOverlay;
    private SkyBox skyBox;
    private Sphere sphere;
    private boolean headUnderWater = false;

    private float time = 1.0f;

    private int modelIndexBuffer, textRowVertexArray;
    private int deferredRenderingFrameBuffer, depthTexture, normalTexture, positionTexture, colorTexture, propertiesTexture;
    private int ssaoFrameBuffer, ssaoTexture, noiseTexture;
    private int lightsFrameBuffer, lightTexture;
    private int finalFrameBuffer;
    private int[] particlesData = new int[1];
    private boolean particlesHaveChanged;
    private int particlesBuffer, opaqueParticleCount = 0, transparentParticleCount = 0;

    private boolean xRay;
}
