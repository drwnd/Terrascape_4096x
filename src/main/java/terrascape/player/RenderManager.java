package terrascape.player;

import org.joml.*;
import org.lwjgl.opengl.GL46;
import terrascape.server.*;
import terrascape.server.Chunk;
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
        GL46.glViewport(0, 0, window.getWidth(), window.getHeight());
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

        ssaoShader.reload();
        postShader.reload();
        lightShader.reload();
        lightPrePassShader.reload();
        sunDepthShader.reload();

        opaqueParticleShader.reload();
        transparentParticleShader.reload();

        System.out.println("Shader reload completed.");
    }

    public void resize() throws IllegalStateException {
        GL46.glDeleteFramebuffers(deferredRenderingFrameBuffer);
        GL46.glDeleteFramebuffers(ssaoFrameBuffer);
        GL46.glDeleteFramebuffers(lightsFrameBuffer);
        GL46.glDeleteFramebuffers(finalFrameBuffer);
        GL46.glDeleteFramebuffers(shadowFrameBuffer);

        GL46.glDeleteTextures(depthTexture);
        GL46.glDeleteTextures(normalTexture);
        GL46.glDeleteTextures(positionTexture);
        GL46.glDeleteTextures(colorTexture);
        GL46.glDeleteTextures(propertiesTexture);
        GL46.glDeleteTextures(ssaoTexture);
        GL46.glDeleteTextures(noiseTexture);
        GL46.glDeleteTextures(lightTexture);
        GL46.glDeleteTextures(finalColorTexture);
        GL46.glDeleteTextures(shadowTexture);

        GL46.glViewport(0, 0, window.getWidth(), window.getHeight());
        createConstantBuffers();
    }


    private void loadShaders() throws Exception {
        opaqueMaterialShader = ShaderManager.createOpaqueMaterialShader();
        transparentMaterialShader = ShaderManager.createTransparentMaterialShader();
        waterMaterialShader = ShaderManager.createWaterMaterialShader();

        skyBoxShader = ShaderManager.createSkyBoxShader();
        GUIShader = ShaderManager.createGUIShader();
        textShader = ShaderManager.createTextShader();

        ssaoShader = ShaderManager.createSSAOShader();
        postShader = ShaderManager.createPostShader();
        lightShader = ShaderManager.createLightShader();
        lightPrePassShader = ShaderManager.createLightPrePassShader();
        sunDepthShader = ShaderManager.createSunDepthShader();

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

        normalTexture = ObjectLoader.create2DTexture(GL46.GL_RGB16F, GL46.GL_RGB, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_FLOAT);
        positionTexture = ObjectLoader.create2DTexture(GL46.GL_RGB16F, GL46.GL_RGB, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_FLOAT);
        colorTexture = ObjectLoader.create2DTexture(GL46.GL_RGBA, GL46.GL_RGBA, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_UNSIGNED_BYTE);
        propertiesTexture = ObjectLoader.create2DTexture(GL46.GL_RED, GL46.GL_RED, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_UNSIGNED_BYTE);
        lightTexture = ObjectLoader.create2DTexture(GL46.GL_RGB, GL46.GL_RGB, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_UNSIGNED_BYTE);
        finalColorTexture = ObjectLoader.create2DTexture(GL46.GL_RGB, GL46.GL_RGB, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_UNSIGNED_BYTE);

        // Needs to be linear otherwise there are artifacts
        depthTexture = ObjectLoader.create2DTexture(GL46.GL_DEPTH32F_STENCIL8, GL46.GL_DEPTH_STENCIL, window.getWidth(), window.getHeight(), GL46.GL_LINEAR, GL46.GL_FLOAT_32_UNSIGNED_INT_24_8_REV);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameterfv(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});

        shadowTexture = ObjectLoader.create2DTexture(GL46.GL_DEPTH_COMPONENT32F, GL46.GL_DEPTH_COMPONENT, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, GL46.GL_NEAREST, GL46.GL_FLOAT);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameterfv(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});

        noiseTexture = GL46.glCreateTextures(GL46.GL_TEXTURE_2D);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, noiseTexture);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGB, 4, 4, 0, GL46.GL_RGB, GL46.GL_FLOAT, noise);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_REPEAT);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_REPEAT);

        ssaoTexture = ObjectLoader.create2DTexture(GL46.GL_RED, GL46.GL_RED, window.getWidth(), window.getHeight(), GL46.GL_NEAREST, GL46.GL_FLOAT);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);

        deferredRenderingFrameBuffer = GL46.glCreateFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, deferredRenderingFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, normalTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT1, GL46.GL_TEXTURE_2D, positionTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT2, GL46.GL_TEXTURE_2D, colorTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT3, GL46.GL_TEXTURE_2D, propertiesTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        GL46.glDrawBuffers(new int[]{GL46.GL_COLOR_ATTACHMENT0, GL46.GL_COLOR_ATTACHMENT1, GL46.GL_COLOR_ATTACHMENT2, GL46.GL_COLOR_ATTACHMENT3});
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));

        ssaoFrameBuffer = GL46.glCreateFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, ssaoFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, ssaoTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("SSAO Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));

        lightsFrameBuffer = GL46.glCreateFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, lightsFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, lightTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Light Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));

        finalFrameBuffer = GL46.glCreateFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, finalFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, finalColorTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Final Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));

        shadowFrameBuffer = GL46.glCreateFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, shadowFrameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_ATTACHMENT, GL46.GL_TEXTURE_2D, shadowTexture, 0);
        GL46.glDrawBuffer(GL46.GL_NONE);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Shadow Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));
    }

    private void loadMiscellaneous() throws Exception {
        textRowVertexArray = ObjectLoader.loadTextRow();
        modelIndexBuffer = ObjectLoader.loadModelIndexBuffer();
        screenOverlay = ObjectLoader.loadGUIElement(OVERLAY_VERTICES, GUI_ELEMENT_TEXTURE_COORDINATES, new Vector2f(0.0f, 0.0f));
        screenOverlay.setTexture(new Texture(ObjectLoader.loadTexture("textures/InventoryOverlay.png")));
        sphere = ObjectLoader.loadUnitSphere(15, 15);
        skyBox = ObjectLoader.loadSkyBox();
    }


    private void bindModel(OpaqueModel model, ShaderManager shader) {
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer);

        shader.setUniform("worldPos", model.X, model.Y, model.Z, 1 << model.LOD);
    }

    private void bindSkyBox(SkyBox skyBox) {
        GL46.glBindVertexArray(skyBox.vao());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, skyBox.nightTexture().id());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, skyBox.dayTexture().id());
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
        Matrix4f sunMatrix = Transformation.getSunMatrix(sunDirection);
        long start;

        start = System.nanoTime();
        computeShadows(sunMatrix);
        if (player.printTimes) System.out.println("compute shadows       " + (System.nanoTime() - start));

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, deferredRenderingFrameBuffer);
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT);
        if (xRay) GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_LINE);
        else GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_FILL);

        start = System.nanoTime();
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
        doDeferredCalculation(sunDirection, passedTicks, sunMatrix);
        if (player.printTimes) System.out.println("deferred calculations " + (System.nanoTime() - start));

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
        particleEffects.clear();
        shadowModels.clear();

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

        GL46.glDrawElements(GL46.GL_TRIANGLES, skyBox.vertexCount(), GL46.GL_UNSIGNED_INT, 0);

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

        GL46.glBindVertexArray(skyBox.vao()); // Just bind something IDK
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.ATLAS.id());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.PROPERTIES_ATLAS.id());

        for (OpaqueModel model : opaqueModels) {
            if (!model.containsGeometry) continue;
            int[] toRenderVertexCounts = model.getVertexCounts(playerChunkX, playerChunkY, playerChunkZ);

            bindModel(model, opaqueMaterialShader);

            GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, model.getIndices(), toRenderVertexCounts);
        }
    }

    private void renderOpaqueParticles(Matrix4f projectionViewMatrix) {
        opaqueParticleShader.bind();
        opaqueParticleShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        Vector3f cameraPosition = player.getCamera().getPosition();
        opaqueParticleShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);
        opaqueParticleShader.setUniform("textureAtlas", 0);
        opaqueParticleShader.setUniform("propertiesTexture", 1);
        opaqueParticleShader.setUniform("currentTime", (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT));

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.ATLAS.id());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, Texture.PROPERTIES_ATLAS.id());
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_CULL_FACE);

        for (ParticleEffect particleEffect : particleEffects) {
            if (!particleEffect.isOpaque()) continue;
            opaqueParticleShader.setUniform("spawnTime", particleEffect.spawnTime());
            opaqueParticleShader.setUniform("startPosition", particleEffect.x(), particleEffect.y(), particleEffect.z());
            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, particleEffect.buffer());
            GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, particleEffect.count());
        }
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

    private void computeShadows(Matrix4f sunMatrix) {
        if (!DO_SHADOW_MAPPING) return;
        GL46.glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, shadowFrameBuffer);
        GL46.glClear(GL46.GL_DEPTH_BUFFER_BIT);

        sunDepthShader.bind();
        sunDepthShader.setUniform("projectionViewMatrix", sunMatrix);
        Vector3f cameraPosition = player.getCamera().getPosition();
        sunDepthShader.setUniform("iCameraPosition",
                Utils.floor(cameraPosition.x) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.y) & ~CHUNK_SIZE_MASK,
                Utils.floor(cameraPosition.z) & ~CHUNK_SIZE_MASK);

        GL46.glBindVertexArray(skyBox.vao()); // Just bind something IDK
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glDepthMask(true);

        for (OpaqueModel model : shadowModels) {
            if (!model.containsGeometry) continue;
            int[] toRenderVertexCounts = model.getAllVertexCounts();

            bindModel(model, sunDepthShader);

            GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, model.getIndices(), toRenderVertexCounts);
        }

        GL46.glViewport(0, 0, window.getWidth(), window.getHeight());
    }

    private void doDeferredCalculation(Vector3f sunDirection, float passedTicks, Matrix4f sunMatrix) {
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
        GL46.glActiveTexture(GL46.GL_TEXTURE6);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, shadowTexture);

        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glDepthMask(false);
        GL46.glColorMask(true, true, true, true);

        postShader.setUniform("time", getRenderTime(passedTicks));
        postShader.setUniform("flags", (headUnderWater ? 1 : 0) | (DO_SHADOW_MAPPING ? 2 : 0));
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
        postShader.setUniform("shadowTexture", 6);
        postShader.setUniform("position", screenOverlay.getPosition());
        postShader.setUniform("screenSize", window.getWidth(), window.getHeight());
        postShader.setUniform("sunMatrix", sunMatrix);

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
        transparentParticleShader.bind();
        transparentParticleShader.setUniform("projectionViewMatrix", projectionViewMatrix);
        transparentParticleShader.setUniform("currentTime", (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT));
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

        for (ParticleEffect particleEffect : particleEffects) {
            if (particleEffect.isOpaque()) continue;
            transparentParticleShader.setUniform("spawnTime", particleEffect.spawnTime());
            transparentParticleShader.setUniform("startPosition", particleEffect.x(), particleEffect.y(), particleEffect.z());
            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, particleEffect.buffer());
            GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, particleEffect.count());
        }
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
        Vector3f sunDirection = Transformation.getSunDirection(getRenderTime(time));

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
        renderTextLine("Rendered particles:" + particleEffects.size(), Color.RED, ++line);
        renderTextLine("Rendered shadow models:" + shadowModels.size(), Color.RED, ++line);
        renderTextLine("Render distance XZ:" + RENDER_DISTANCE_XZ + " Render distance Y:" + RENDER_DISTANCE_Y, Color.ORANGE, ++line);
        renderTextLine("Concurrent played sounds:" + sourceCounter, Color.YELLOW, ++line);
        renderTextLine("Tick:" + EngineManager.getTick() + " Time:" + time, Color.WHITE, ++line);
        renderTextLine("To buffer chunks:" + ServerLogic.getAmountOfToBufferChunks(), Color.RED, ++line);
        renderTextLine("Hei:" + heightMapValue + " Ero:" + erosionMapValue + " Con:" + continentalMapValue, Color.GRAY, ++line);
        renderTextLine("Riv:" + riverMapValue + " Rid:" + ridgeMapValue, Color.GRAY, ++line);
        renderTextLine("Tem:" + temperatureMapValue + " Hum:" + humidityMapValue, Color.GRAY, ++line);
        renderTextLine("Resulting height:" + WorldGeneration.getResultingHeight(heightMapValue, erosionMapValue, continentalMapValue, riverMapValue, ridgeMapValue), Color.GRAY, ++line);
        renderTextLine("Biome: " + WorldGeneration.getBiome(temperatureMapValue, humidityMapValue, 96, resultingHeight, erosionMapValue, continentalMapValue).getName(), Color.GRAY, ++line);
        renderTextLine("Sun direction: x:" + sunDirection.x + " y:" + sunDirection.y + " z:" + sunDirection.z, Color.WHITE, ++line);

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


    public void processShadowModel(OpaqueModel shadowModel) {
        shadowModels.add(shadowModel);
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

    public void processParticleEffect(ParticleEffect particleEffect) {
        particleEffects.add(particleEffect);
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


    private final WindowManager window;
    private ShaderManager opaqueMaterialShader, transparentMaterialShader, waterMaterialShader;
    private ShaderManager opaqueParticleShader, transparentParticleShader;
    private ShaderManager skyBoxShader, GUIShader, textShader;
    private ShaderManager ssaoShader, postShader, lightShader, lightPrePassShader, sunDepthShader;

    private final ArrayList<OpaqueModel> opaqueModels = new ArrayList<>();
    private final ArrayList<TransparentModel> transparentModels = new ArrayList<>();
    private final ArrayList<LightModel> lightModels = new ArrayList<>();
    private final ArrayList<GUIElement> GUIElements = new ArrayList<>();
    private final ArrayList<DisplayString> displayStrings = new ArrayList<>();
    private final ArrayList<ParticleEffect> particleEffects = new ArrayList<>();
    private final ArrayList<OpaqueModel> shadowModels = new ArrayList<>();
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
    private int shadowFrameBuffer, shadowTexture;
    private int finalFrameBuffer, finalColorTexture;

    private boolean xRay;
}
