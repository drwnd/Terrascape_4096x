package terrascape.player;

import terrascape.dataStorage.FileManager;
import terrascape.entity.*;
import terrascape.dataStorage.octree.Chunk;
import terrascape.generation.WorldGeneration;
import terrascape.server.EngineManager;
import terrascape.server.Material;
import terrascape.utils.Transformation;
import terrascape.utils.Utils;
import terrascape.server.Launcher;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;

import static terrascape.generation.WorldGeneration.WATER_LEVEL;
import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

public final class Player {

    public Player() {
        window = Launcher.getWindow();
        sound = Launcher.getSound();
        renderer = new RenderManager(this);
        camera = new Camera();
        mouseInput = new MouseInput(this);
        movement = new Movement(this);
        interactionHandler = new InteractionHandler(this);

        int spawnX = 0;
        int spawnZ = 0;

        for (int counter = 0; counter < 100 && WorldGeneration.getResultingHeight(Utils.floor(spawnX), Utils.floor(spawnZ)) < WATER_LEVEL; counter++) {
            spawnX = Utils.floor(Math.random() * SPAWN_RADIUS * 2 - SPAWN_RADIUS);
            spawnZ = Utils.floor(Math.random() * SPAWN_RADIUS * 2 - SPAWN_RADIUS);
        }

        camera.setPosition(spawnX + 0.5f, WorldGeneration.getResultingHeight(spawnX, spawnZ) + 3, spawnZ + 0.5f);
        visibleChunks = new long[LOD_COUNT][(RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH >> 6) + 1];
    }

    public void init() throws Exception {
        Texture skyBoxTexture1 = new Texture(ObjectLoader.loadTexture("textures/706c5e1da58f47ad6e18145165caf55d.png"));
        Texture skyBoxTexture2 = new Texture(ObjectLoader.loadTexture("textures/82984-skybox-blue-atmosphere-sky-space-hd-image-free-png.png"));
        SkyBox skyBox = ObjectLoader.loadSkyBox(SKY_BOX_VERTICES, SKY_BOX_TEXTURE_COORDINATES, SKY_BOX_INDICES, camera.getPosition());
        skyBox.setTexture(skyBoxTexture1, skyBoxTexture2);
        renderer.processSkyBox(skyBox);

        GUIElement.loadGUIElements(this);

        GUIElement inventoryOverlay = ObjectLoader.loadGUIElement(OVERLAY_VERTICES, GUI_ELEMENT_TEXTURE_COORDINATES, new Vector2f(0.0f, 0.0f));
        inventoryOverlay.setTexture(new Texture(ObjectLoader.loadTexture("textures/InventoryOverlay.png")));
        renderer.setInventoryOverlay(inventoryOverlay);

        updateHotBarElements();

        mouseInput.init();

        GLFW.glfwSetKeyCallback(window.getWindow(), (long window, int key, int scancode, int action, int mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                if (!isInInventory()) GLFW.glfwSetWindowShouldClose(window, true);
                else toggleInventory();
                return;
            }
            handleNonMovementInputs(key | IS_KEYBOARD_BUTTON, action);

            // Debug
            if (key == GLFW.GLFW_KEY_B && action == GLFW.GLFW_PRESS) {
                renderer.setTime(0.0f);
            }
            if (key == GLFW.GLFW_KEY_N && action == GLFW.GLFW_PRESS) {
                renderer.setTime(0.5f);
            }
            if (key == GLFW.GLFW_KEY_M && action == GLFW.GLFW_PRESS) {
                renderer.setTime(1.0f);
            }
            if (key == GLFW.GLFW_KEY_J && action == GLFW.GLFW_PRESS) {
                printTimes = !printTimes;
            }
            if (key == GLFW.GLFW_KEY_L && action == GLFW.GLFW_PRESS) {
                renderingEntities = !renderingEntities;
                System.out.println("rendering entities " + renderingEntities);
            }
            if (key == GLFW.GLFW_KEY_K && action == GLFW.GLFW_PRESS) {
                for (int lod = 0; lod < LOD_COUNT; lod++)
                    System.out.println("LOD: " + lod + " " + Chunk.getByteSize(lod) / 1_000_000 + "MB");
            }
            if (key == TOGGLE_NO_CLIP_BUTTON && action == GLFW.GLFW_PRESS) noClip = !noClip;
            if (key == TOGGLE_X_RAY_BUTTON && action == GLFW.GLFW_PRESS) renderer.setXRay(!renderer.isxRay());
            if (key == RELOAD_SHADERS_BUTTON && action == GLFW.GLFW_PRESS) renderer.reloadShaders();
        });
    }

    public void update(float passedTicks) {
        Vector3f velocity = movement.getVelocity();
        movement.moveCameraHandleCollisions(velocity.x * passedTicks, velocity.y * passedTicks, velocity.z * passedTicks);

        mouseInput.input();
        Vector2f rotVec = mouseInput.getDisplayVec();
        if (!inInventory) camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY);

        sound.setListenerData(this);
    }

    public void updateGT() {
        renderer.incrementTime();
        playFootstepsSounds();
        handleWaterSplashEffects();
    }

    private void playFootstepsSounds() {
        if (!window.isKeyPressed(MOVE_FORWARD_BUTTON) && !window.isKeyPressed(MOVE_BACK_BUTTON)
                && !window.isKeyPressed(MOVE_LEFT_BUTTON) && !window.isKeyPressed(MOVE_RIGHT_BUTTON)) return;

        long tick = EngineManager.getTick();
        int movementState = movement.getMovementState();
        if (movementState == Movement.SWIMMING) {
            if (tick - lastFootstepTick < 15) return;
            Vector3f position = camera.getPosition();
            sound.playRandomSound(sound.swim, position.x, position.y, position.z, 0.0f, 0.0f, 0.0f, STEP_GAIN);
            lastFootstepTick = tick;
            return;
        }
        Vector3f velocity = movement.getVelocity();

        if (!movement.isGrounded() || Math.abs(velocity.x) < 0.001f && Math.abs(velocity.z) < 0.001f) return;
        if (movementState == Movement.CROUCHING || movementState == Movement.CRAWLING) return;
        if (movementState == Movement.WALKING) {
            if (window.isKeyPressed(SPRINT_BUTTON)) {
                if (tick - lastFootstepTick < 5) return;
            } else if (tick - lastFootstepTick < 10) return;
        }
        lastFootstepTick = tick;

        Vector3f position = camera.getPosition();
        float height = Movement.PLAYER_FEET_OFFSETS[movementState];
        byte standingMaterial = movement.getStandingMaterial();
        sound.playRandomSound(Material.getFootstepsSound(standingMaterial), position.x, position.y - height, position.z, 0.0f, 0.0f, 0.0f, STEP_GAIN);
    }

    private void handleWaterSplashEffects() {
        Vector3f position = camera.getPosition();
        boolean touchingWater = movement.collidesWithWater(position.x, position.y, position.z, movement.getMovementState());

        if (touchingWater != this.touchingWater)
            Particle.addSplashParticle(Utils.floor(position.x), Utils.floor(position.y - Movement.PLAYER_FEET_OFFSETS[movement.getMovementState()]), Utils.floor(position.z), WATER);

        this.touchingWater = touchingWater;
    }

    public void input() {
        Vector3f position = camera.getPosition();
        boolean isInWater = movement.collidesWithWater(position.x, position.y, position.z, movement.getMovementState());
        movement.move();

        interactionHandler.handleDestroyUsePickMaterialInput();
        if (inInventory) handleInventoryHotkeys();

        if (isInWater != movement.isTouchingWater()) {
            sound.playRandomSound(sound.splash, position.x, position.y, position.z, 0.0f, 0.0f, 0.0f, MISCELLANEOUS_GAIN);
        }
        movement.setTouchingWater(isInWater);
    }

    private void handleInventoryHotkeys() {
        if (window.isKeyPressed(HOT_BAR_SLOT_1)) hotBar[0] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        else if (window.isKeyPressed(HOT_BAR_SLOT_2)) hotBar[1] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        else if (window.isKeyPressed(HOT_BAR_SLOT_3)) hotBar[2] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        else if (window.isKeyPressed(HOT_BAR_SLOT_4)) hotBar[3] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        else if (window.isKeyPressed(HOT_BAR_SLOT_5)) hotBar[4] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        else if (window.isKeyPressed(HOT_BAR_SLOT_6)) hotBar[5] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        else if (window.isKeyPressed(HOT_BAR_SLOT_7)) hotBar[6] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        else if (window.isKeyPressed(HOT_BAR_SLOT_8)) hotBar[7] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        else if (window.isKeyPressed(HOT_BAR_SLOT_9)) hotBar[8] = GUIElement.getHoveredOverMaterial(inventoryScroll);
        updateHotBarElements();
    }

    public void handleNonMovementInputs(int button, int action) {
        if (button == ZOOM_BUTTON && action == GLFW.GLFW_PRESS) {
            camera.setZoomModifier(0.25f);
        } else if (button == ZOOM_BUTTON && action == GLFW.GLFW_RELEASE) window.updateProjectionMatrix(FOV);

        if (action != GLFW.GLFW_PRESS) return;

        if (button == HOT_BAR_SLOT_1) setSelectedHotBarSlot(0);
        else if (button == HOT_BAR_SLOT_2) setSelectedHotBarSlot(1);
        else if (button == HOT_BAR_SLOT_3) setSelectedHotBarSlot(2);
        else if (button == HOT_BAR_SLOT_4) setSelectedHotBarSlot(3);
        else if (button == HOT_BAR_SLOT_5) setSelectedHotBarSlot(4);
        else if (button == HOT_BAR_SLOT_6) setSelectedHotBarSlot(5);
        else if (button == HOT_BAR_SLOT_7) setSelectedHotBarSlot(6);
        else if (button == HOT_BAR_SLOT_8) setSelectedHotBarSlot(7);
        else if (button == HOT_BAR_SLOT_9) setSelectedHotBarSlot(8);

        else if (button == OPEN_INVENTORY_BUTTON) toggleInventory();
        else if (button == OPEN_DEBUG_MENU_BUTTON) debugScreenOpen = !debugScreenOpen;
        else if (button == INCREASE_BREAK_PLACE_SIZE_BUTTON) interactionHandler.incBreakingPlacingSize();
        else if (button == DECREASE_BREAK_PLACE_SIZE_BUTTON) interactionHandler.decBreakingPlacingSize();

        else if (button == RELOAD_SETTINGS_BUTTON) try {
            FileManager.loadSettings(false);
        } catch (Exception e) {
            System.err.println("Invalid settings file");
        }
    }

    public void setSelectedHotBarSlot(int slot) {
        if (selectedHotBarSlot == slot) return;

        selectedHotBarSlot = slot;
        hotBarSelectionIndicator.setPosition(new Vector2f((slot - 4) * 40 * GUI_SIZE / Launcher.getWindow().getWidth(), 0.0f));
        Vector3f position = camera.getPosition();
        Vector3f velocity = movement.getVelocity();
        sound.playRandomSound(Material.getFootstepsSound(hotBar[selectedHotBarSlot]), position.x, position.y, position.z, velocity.x, velocity.y, velocity.z, INVENTORY_GAIN);
    }

    private void toggleInventory() {
        inInventory = !inInventory;
        GLFW.glfwSetInputMode(window.getWindow(), GLFW.GLFW_CURSOR, inInventory ? GLFW.GLFW_CURSOR_NORMAL : GLFW.GLFW_CURSOR_DISABLED);
    }

    public void render(float passedTicks) {
        long playerTime = System.nanoTime();
        Vector3f cameraPosition = camera.getPosition();
        final int playerChunkX = Utils.floor(cameraPosition.x) >> CHUNK_SIZE_BITS;
        final int playerChunkY = Utils.floor(cameraPosition.y) >> CHUNK_SIZE_BITS;
        final int playerChunkZ = Utils.floor(cameraPosition.z) >> CHUNK_SIZE_BITS;

        long cullingTime = System.nanoTime();
        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera, window);
        FrustumIntersection frustumIntersection = new FrustumIntersection(projectionViewMatrix);
        for (int lod = 0; lod < LOD_COUNT; lod++)
            calculateCulling(playerChunkX, playerChunkY, playerChunkZ, lod, frustumIntersection);
        cullingTime = System.nanoTime() - cullingTime;

        long queuingTime = System.nanoTime();
        queueModelsForRendering(playerChunkX, playerChunkY, playerChunkZ);
        queuingTime = System.nanoTime() - queuingTime;

        renderGUIElements();
        Particle.renderParticles(renderer);

        boolean headUnderWater = Chunk.getMaterialInWorld(Utils.floor(cameraPosition.x), Utils.floor(cameraPosition.y), Utils.floor(cameraPosition.z)) == WATER;
        if (headUnderWater && !this.headUnderWater)
            sound.playRandomSound(sound.submerge, cameraPosition.x, cameraPosition.y, cameraPosition.z, 0.0f, 0.0f, 0.0f, MISCELLANEOUS_GAIN);
        else if (this.headUnderWater && !headUnderWater)
            sound.playRandomSound(sound.splash, cameraPosition.x, cameraPosition.y, cameraPosition.z, 0.0f, 0.0f, 0.0f, MISCELLANEOUS_GAIN);
        this.headUnderWater = headUnderWater;
        renderer.setHeadUnderWater(headUnderWater);

        if (inInventory) renderInventoryElements();
        playerTime = System.nanoTime() - playerTime;

        long renderTime = System.nanoTime();
        renderer.render(camera, passedTicks);
        renderTime = System.nanoTime() - renderTime;

        if (printTimes) {
            System.out.println("culling Time " + cullingTime);
            System.out.println("queuing Time " + queuingTime);
            System.out.println("total player " + playerTime);
            System.out.println("total render " + renderTime);
            System.out.println("total cpu    " + (renderTime + playerTime));
        }
    }

    private void renderGUIElements() {
        for (GUIElement GUIElement : GUIElements) renderer.processGUIElement(GUIElement);
        for (GUIElement GUIElement : hotBarElements) renderer.processGUIElement(GUIElement);
        renderer.processGUIElement(hotBarSelectionIndicator);

        int width = window.getWidth();
        int height = window.getHeight();

        renderer.processDisplayString(new DisplayString((int) (width * 0.6), (int) (height * 0.97), (1 << interactionHandler.getBreakingPlacingSize()) + " pixel"));
    }

    private void queueModelsForRendering(int playerChunkX, int playerChunkY, int playerChunkZ) {
        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) {
            int lodPlayerX = playerChunkX >> lod;
            int lodPlayerY = playerChunkY >> lod;
            int lodPlayerZ = playerChunkZ >> lod;

            int endX = Utils.makeOdd(lodPlayerX + RENDER_DISTANCE_XZ + 2);
            int endZ = Utils.makeOdd(lodPlayerZ + RENDER_DISTANCE_XZ + 2);
            int endY = Utils.makeOdd(lodPlayerY + RENDER_DISTANCE_Y + 2);

            for (int lodModelX = Utils.mackEven(lodPlayerX - RENDER_DISTANCE_XZ - 2); lodModelX <= endX; lodModelX++)
                for (int lodModelZ = Utils.mackEven(lodPlayerZ - RENDER_DISTANCE_XZ - 2); lodModelZ <= endZ; lodModelZ++)
                    for (int lodModelY = Utils.mackEven(lodPlayerY - RENDER_DISTANCE_Y - 2); lodModelY <= endY; lodModelY++)
                        queueModelForRendering(lodModelX, lodModelY, lodModelZ, lod);
        }
    }

    private void queueModelForRendering(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ);
        if ((visibleChunks[lod][index >> 6] & 1L << (index & 63)) == 0) return;

        OpaqueModel opaqueModel = Chunk.getOpaqueModel(index, lod);
        WaterModel waterModel = Chunk.getWaterModel(index, lod);
        if (opaqueModel == null || waterModel == null) return;

        if (lod == 0 || modelFarEnoughAway(lodModelX, lodModelY, lodModelZ, lod)) {
            renderer.processOpaqueModel(opaqueModel);
            renderer.processWaterModel(waterModel);
            return;
        }

        int nextLodX = lodModelX << 1;
        int nextLodY = lodModelY << 1;
        int nextLodZ = lodModelZ << 1;
        if (modelCubePresent(nextLodX, nextLodY, nextLodZ, lod - 1)) return;

        clearModelCubeVisibility(nextLodX, nextLodY, nextLodZ, lod - 1);
        renderer.processOpaqueModel(opaqueModel);
        renderer.processWaterModel(waterModel);
    }

    private boolean modelFarEnoughAway(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        Vector3f position = camera.getPosition();

        int distanceX = Math.abs((Utils.floor(position.x) >> CHUNK_SIZE_BITS + lod) - lodModelX);
        int distanceY = Math.abs((Utils.floor(position.y) >> CHUNK_SIZE_BITS + lod) - lodModelY);
        int distanceZ = Math.abs((Utils.floor(position.z) >> CHUNK_SIZE_BITS + lod) - lodModelZ);

        return distanceX > RENDER_DISTANCE_XZ / 2 + 1 || distanceZ > RENDER_DISTANCE_XZ / 2 + 1 || distanceY > RENDER_DISTANCE_Y / 2 + 1;
    }

    private static boolean modelCubePresent(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        return Chunk.isModelPresent(lodModelX, lodModelY, lodModelZ, lod)
                && Chunk.isModelPresent(lodModelX, lodModelY, lodModelZ + 1, lod)
                && Chunk.isModelPresent(lodModelX, lodModelY + 1, lodModelZ, lod)
                && Chunk.isModelPresent(lodModelX, lodModelY + 1, lodModelZ + 1, lod)
                && Chunk.isModelPresent(lodModelX + 1, lodModelY, lodModelZ, lod)
                && Chunk.isModelPresent(lodModelX + 1, lodModelY, lodModelZ + 1, lod)
                && Chunk.isModelPresent(lodModelX + 1, lodModelY + 1, lodModelZ, lod)
                && Chunk.isModelPresent(lodModelX + 1, lodModelY + 1, lodModelZ + 1, lod);
    }

    private void clearModelCubeVisibility(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        long[] visibleChunks = this.visibleChunks[lod];
        int index;
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ);
        visibleChunks[index >> 6] &= ~(1L << (index & 63));
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ + 1);
        visibleChunks[index >> 6] &= ~(1L << (index & 63));
        index = Utils.getChunkIndex(lodModelX, lodModelY + 1, lodModelZ);
        visibleChunks[index >> 6] &= ~(1L << (index & 63));
        index = Utils.getChunkIndex(lodModelX, lodModelY + 1, lodModelZ + 1);
        visibleChunks[index >> 6] &= ~(1L << (index & 63));
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ);
        visibleChunks[index >> 6] &= ~(1L << (index & 63));
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ + 1);
        visibleChunks[index >> 6] &= ~(1L << (index & 63));
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY + 1, lodModelZ);
        visibleChunks[index >> 6] &= ~(1L << (index & 63));
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY + 1, lodModelZ + 1);
        visibleChunks[index >> 6] &= ~(1L << (index & 63));
    }

    private void renderInventoryElements() {
        for (GUIElement element : inventoryElements) {
            if (element.getPosition().y > 0.55f || element.getPosition().y < -0.55f) continue;
            renderer.processGUIElement(element);
        }
        byte hoveredMaterial = GUIElement.getHoveredOverMaterial(inventoryScroll);
        if (hoveredMaterial == AIR) return;
        String name = Material.getMaterialName(hoveredMaterial);
        renderer.processDisplayString(new DisplayString(mouseInput.getX() - name.length() * TEXT_CHAR_SIZE_X, mouseInput.getY(), name));
    }

    private void calculateCulling(int playerChunkX, int playerChunkY, int playerChunkZ, int lod, FrustumIntersection frustumIntersection) {
        Arrays.fill(visibleChunks[lod], 0);
        playerChunkX >>= lod;
        playerChunkY >>= lod;
        playerChunkZ >>= lod;

        int chunkIndex = Utils.getChunkIndex(playerChunkX, playerChunkY, playerChunkZ);
        visibleChunks[lod][chunkIndex >> 6] = visibleChunks[lod][chunkIndex >> 6] | 1L << (chunkIndex & 63);

        fillVisibleChunks(playerChunkX, playerChunkY, playerChunkZ + 1, (byte) (1 << NORTH), lod, frustumIntersection);
        fillVisibleChunks(playerChunkX, playerChunkY, playerChunkZ - 1, (byte) (1 << SOUTH), lod, frustumIntersection);

        fillVisibleChunks(playerChunkX, playerChunkY + 1, playerChunkZ, (byte) (1 << TOP), lod, frustumIntersection);
        fillVisibleChunks(playerChunkX, playerChunkY - 1, playerChunkZ, (byte) (1 << BOTTOM), lod, frustumIntersection);

        fillVisibleChunks(playerChunkX + 1, playerChunkY, playerChunkZ, (byte) (1 << WEST), lod, frustumIntersection);
        fillVisibleChunks(playerChunkX - 1, playerChunkY, playerChunkZ, (byte) (1 << EAST), lod, frustumIntersection);

    }

    private void fillVisibleChunks(int chunkX, int chunkY, int chunkZ, byte traveledDirections, int lod, FrustumIntersection intersection) {
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;

        int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ);
        if ((visibleChunks[lod][chunkIndex >> 6] & 1L << (chunkIndex & 63)) != 0) return;

        if (Chunk.getChunk(chunkIndex, lod) == null) return;
        int intersectionType = intersection.intersectAab(chunkX << chunkSizeBits, chunkY << chunkSizeBits, chunkZ << chunkSizeBits, chunkX + 1 << chunkSizeBits, chunkY + 1 << chunkSizeBits, chunkZ + 1 << chunkSizeBits);
        if (intersectionType != FrustumIntersection.INSIDE && intersectionType != FrustumIntersection.INTERSECT) return;

        visibleChunks[lod][chunkIndex >> 6] |= 1L << (chunkIndex & 63);

        if ((traveledDirections & 1 << SOUTH) == 0)
            fillVisibleChunks(chunkX, chunkY, chunkZ + 1, (byte) (traveledDirections | 1 << NORTH), lod, intersection);
        if ((traveledDirections & 1 << NORTH) == 0)
            fillVisibleChunks(chunkX, chunkY, chunkZ - 1, (byte) (traveledDirections | 1 << SOUTH), lod, intersection);

        if ((traveledDirections & 1 << BOTTOM) == 0)
            fillVisibleChunks(chunkX, chunkY + 1, chunkZ, (byte) (traveledDirections | 1 << TOP), lod, intersection);
        if ((traveledDirections & 1 << TOP) == 0)
            fillVisibleChunks(chunkX, chunkY - 1, chunkZ, (byte) (traveledDirections | 1 << BOTTOM), lod, intersection);

        if ((traveledDirections & 1 << EAST) == 0)
            fillVisibleChunks(chunkX + 1, chunkY, chunkZ, (byte) (traveledDirections | 1 << WEST), lod, intersection);
        if ((traveledDirections & 1 << WEST) == 0)
            fillVisibleChunks(chunkX - 1, chunkY, chunkZ, (byte) (traveledDirections | 1 << EAST), lod, intersection);
    }

    public void updateHotBarElements() {
        for (GUIElement element : hotBarElements) {
            if (element == null) continue;
            ObjectLoader.removeVAO(element.getVao());
            ObjectLoader.removeVBO(element.getVbo1());
            ObjectLoader.removeVBO(element.getVbo2());
        }
        hotBarElements.clear();

        int width = window.getWidth();
        int height = window.getHeight();

        for (int i = 0; i < hotBar.length; i++) {
            float xOffset = (40.0f * i - 165 + 4) * GUI_SIZE / width;
            float yOffset = -0.5f + 4.0f * GUI_SIZE / height;
            GUIElement element;

            byte material = hotBar[i];

            byte textureIndex = Material.getTextureIndex(material);
            float[] textureCoordinates = GUIElement.getMaterialDisplayTextureCoordinates(textureIndex, material);
            element = ObjectLoader.loadGUIElement(GUIElement.getMaterialDisplayVertices(material), textureCoordinates, new Vector2f(xOffset, yOffset));

            element.setTexture(Texture.ATLAS);
            hotBarElements.add(element);
        }
    }

    public RenderManager getRenderer() {
        return renderer;
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean hasCollision() {
        return !noClip;
    }

    public boolean isInInventory() {
        return inInventory;
    }

    public void updateInventoryScroll(float value) {
        float maxScroll = GUI_SIZE * 0.04f * (1 + AMOUNT_OF_MATERIALS) - 1.0f;

        if (inventoryScroll + value < 0.0f) value = -inventoryScroll;
        if (inventoryScroll + value > maxScroll) value = maxScroll - inventoryScroll;

        inventoryScroll += value;

        for (GUIElement element : inventoryElements) {
            element.getPosition().add(0.0f, value);
        }
    }

    public boolean isDebugScreenOpen() {
        return debugScreenOpen;
    }

    public int getSelectedHotBarSlot() {
        return selectedHotBarSlot;
    }

    public byte[] getHotBar() {
        return hotBar;
    }

    public void setHotBar(byte[] hotBar) {
        this.hotBar = hotBar;
        updateHotBarElements();
    }

    public float getInventoryScroll() {
        return inventoryScroll;
    }

    public void cleanUp() {
        renderer.cleanUp();
    }

    public Movement getMovement() {
        return movement;
    }

    public InteractionHandler getInteractionHandler() {
        return interactionHandler;
    }

    public ArrayList<GUIElement> getInventoryElements() {
        return inventoryElements;
    }

    public ArrayList<GUIElement> getGUIElements() {
        return GUIElements;
    }

    public GUIElement getHotBarSelectionIndicator() {
        return hotBarSelectionIndicator;
    }

    public void setHotBarSelectionIndicator(GUIElement hotBarSelectionIndicator) {
        this.hotBarSelectionIndicator = hotBarSelectionIndicator;
    }

    public void setInventoryScroll(float inventoryScroll) {
        this.inventoryScroll = inventoryScroll;
    }

    private final RenderManager renderer;
    private final WindowManager window;
    private final SoundManager sound;
    private final Camera camera;
    private final MouseInput mouseInput;
    private final Movement movement;
    private final InteractionHandler interactionHandler;

    private final long[][] visibleChunks;

    private final ArrayList<GUIElement> GUIElements = new ArrayList<>();
    private final ArrayList<GUIElement> hotBarElements = new ArrayList<>();
    private final ArrayList<GUIElement> inventoryElements = new ArrayList<>();
    private GUIElement hotBarSelectionIndicator;

    private float inventoryScroll = 0;
    private boolean headUnderWater, touchingWater = false;
    private byte[] hotBar = new byte[9];
    private int selectedHotBarSlot = -1; // No idea but when it's 0 there is a bug but anything else works
    private long lastFootstepTick = 0;
    private boolean inInventory;

    // Debug
    private boolean debugScreenOpen;
    private boolean renderingEntities = true;
    public boolean printTimes = false;
    private boolean noClip;
}