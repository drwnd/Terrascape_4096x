package terrascape.player;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;
import terrascape.dataStorage.octree.Chunk;
import terrascape.entity.Particle;
import terrascape.entity.Target;
import terrascape.server.Material;
import terrascape.server.ServerLogic;
import terrascape.server.Launcher;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Constants.AIR;
import static terrascape.utils.Settings.*;

public final class InteractionHandler {

    public InteractionHandler(Player player) {
        this.player = player;
        camera = player.getCamera();
        window = Launcher.getWindow();
    }

    public void handleDestroyUsePickMaterialInput() {
        if (player.isInInventory()) return;
        boolean useButtonWasJustPressed = this.useButtonWasJustPressed;
        boolean destroyButtonWasJustPressed = this.destroyButtonWasJustPressed;
        this.useButtonWasJustPressed = false;
        this.destroyButtonWasJustPressed = false;
        long currentTime = System.nanoTime();

        handleDestroy(currentTime, destroyButtonWasJustPressed);

        handleUse(currentTime, useButtonWasJustPressed);

        handlePickMaterial();
    }

    public void input(int button, int action) {
        if (button == DESTROY_BUTTON) {
            if (action == GLFW.GLFW_PRESS) {
                destroyButtonPressTime = System.nanoTime();
                destroyButtonWasJustPressed = true;
            } else {
                destroyButtonPressTime = -1;
            }
        } else if (button == USE_BUTTON) {
            if (action == GLFW.GLFW_PRESS) {
                useButtonPressTime = System.nanoTime();
                useButtonWasJustPressed = true;
            } else {
                useButtonPressTime = -1;
            }
        }
    }

    private void handleDestroy(long currentTime, boolean destroyButtonWasJustPressed) {
        if ((destroyButtonPressTime == -1 || currentTime - destroyButtonPressTime <= 300_000_000) && !destroyButtonWasJustPressed)
            return;
        Target target = Target.getTarget(camera.getPosition(), camera.getDirection());
        if (target != null) {
            handleBreakPlaceEffects(target.position().x, target.position().y, target.position().z, AIR, breakingPlacingSize);
            ServerLogic.placeMaterial(AIR, target.position().x, target.position().y, target.position().z, breakingPlacingSize);
        }
    }

    private void handleUse(long currentTime, boolean useButtonWasJustPressed) {
        if (((useButtonPressTime == -1 || currentTime - useButtonPressTime <= 300_000_000) && !useButtonWasJustPressed))
            return;

        Vector3f cameraDirection = camera.getDirection();
        Vector3f cameraPosition = camera.getPosition();
        Target target = Target.getTarget(cameraPosition, cameraDirection);
        if (target == null) return;

        byte selectedMaterial = player.getHotBar()[player.getSelectedHotBarSlot()];

        if (selectedMaterial == AIR) return;

        Vector3i position = target.position();
        int x = position.x;
        int y = position.y;
        int z = position.z;

        if ((Material.getMaterialProperties(Chunk.getMaterialInWorld(x, y, z)) & REPLACEABLE) == 0) {
            byte[] normal = Material.NORMALS[target.side()];
            x = position.x + normal[0];
            y = position.y + normal[1];
            z = position.z + normal[2];
        }
        if (player.hasCollision() && playerCollidesWithMaterial(x, y, z, selectedMaterial, breakingPlacingSize)) return;

        if ((Material.getMaterialProperties(Chunk.getMaterialInWorld(x, y, z)) & REPLACEABLE) != 0) {
            handleBreakPlaceEffects(x, y, z, selectedMaterial, breakingPlacingSize);
            ServerLogic.placeMaterial(selectedMaterial, x, y, z, breakingPlacingSize);
        }
    }

    private static void handleBreakPlaceEffects(int x, int y, int z, byte material, int size) {
        SoundManager sound = Launcher.getSound();
        byte previousMaterial = Chunk.getMaterialInWorld(x, y, z);
        boolean previousMaterialWaterLogged = Material.isWaterMaterial(previousMaterial);
        boolean newMaterialWaterLogged = Material.isWaterMaterial(material);

        if (previousMaterialWaterLogged || !newMaterialWaterLogged) {
            sound.playRandomSound(Material.getDigSound(previousMaterial), x + 0.5f, y + 0.5f, z + 0.5f, 0.0f, 0.0f, 0.0f, DIG_GAIN);
            sound.playRandomSound(Material.getFootstepsSound(material), x + 0.5f, y + 0.5f, z + 0.5f, 0.0f, 0.0f, 0.0f, PLACE_GAIN);
        } else
            sound.playRandomSound(Material.getFootstepsSound(WATER), x + 0.5f, y + 0.5f, z + 0.5f, 0.0f, 0.0f, 0.0f, PLACE_GAIN);

        if (material == AIR) {
            int sideLength = 1 << size;
            int mask = -(1 << size);

            int startX = x & mask;
            int startY = y & mask;
            int startZ = z & mask;

            for (int particleX = startX; particleX < startX + sideLength; particleX++)
                for (int particleY = startY; particleY < startY + sideLength; particleY++)
                    for (int particleZ = startZ; particleZ < startZ + sideLength; particleZ++) {
                        byte particleMaterial = Chunk.getMaterialInWorld(particleX, particleY, particleZ);
                        if (particleMaterial != AIR && particleMaterial != OUT_OF_WORLD)
                            Particle.addBreakParticle(particleX, particleY, particleZ, particleMaterial);
                    }
        }
    }


    public void incBreakingPlacingSize() {
        breakingPlacingSize = Math.min(MAX_BREAKING_PLACING_SIZE, breakingPlacingSize + 1);
    }

    public void decBreakingPlacingSize() {
        breakingPlacingSize = Math.max(MIN_BREAKING_PLACING_SIZE, breakingPlacingSize - 1);
    }

    public int getBreakingPlacingSize() {
        return breakingPlacingSize;
    }

    private void handlePickMaterial() {
        if (!window.isKeyPressed(PICK_MATERIAL_BUTTON)) return;

        Target target = Target.getTarget(camera.getPosition(), camera.getDirection());
        if (target == null) return;

        int selectedHotBarSlot = player.getSelectedHotBarSlot();
        byte[] hotBar = player.getHotBar();
        byte material = Chunk.getMaterialInWorld(target.position().x, target.position().y, target.position().z);

        boolean hasPlacedMaterial = false;
        for (int hotBarSlot = 0; hotBarSlot < hotBar.length; hotBarSlot++) {
            if (hotBar[hotBarSlot] != material) continue;
            hasPlacedMaterial = true;
            if (hotBarSlot != selectedHotBarSlot) player.setSelectedHotBarSlot(hotBarSlot);
            break;
        }
        if (!hasPlacedMaterial && hotBar[selectedHotBarSlot] != AIR)
            for (int hotBarSlot = 0; hotBarSlot < hotBar.length; hotBarSlot++) {
                if (hotBar[hotBarSlot] != AIR) continue;
                hotBar[hotBarSlot] = material;
                if (hotBarSlot != selectedHotBarSlot) player.setSelectedHotBarSlot(hotBarSlot);
                hasPlacedMaterial = true;
                break;
            }
        if (!hasPlacedMaterial) hotBar[selectedHotBarSlot] = material;

        player.updateHotBarElements();
    }

    private boolean playerCollidesWithMaterial(int materialX, int materialY, int materialZ, byte material, int size) {
        if ((Material.getMaterialProperties(material) & NO_COLLISION) != 0) return false;

        Vector3f cameraPosition = camera.getPosition();
        final float minX = cameraPosition.x - Movement.HALF_PLAYER_WIDTH;
        final float maxX = cameraPosition.x + Movement.HALF_PLAYER_WIDTH;
        final float minY = cameraPosition.y - Movement.PLAYER_FEET_OFFSETS[player.getMovement().getMovementState()];
        final float maxY = cameraPosition.y + Movement.PLAYER_HEAD_OFFSET;
        final float minZ = cameraPosition.z - Movement.HALF_PLAYER_WIDTH;
        final float maxZ = cameraPosition.z + Movement.HALF_PLAYER_WIDTH;

        size = 1 << size;
        int mask = -size;
        materialX &= mask;
        materialY &= mask;
        materialZ &= mask;

        return minX < materialX + size && maxX > materialX &&
                minY < materialY + size && maxY > materialY &&
                minZ < materialZ + size && maxZ > materialZ;
    }

    private long useButtonPressTime = -1, destroyButtonPressTime = -1;
    private boolean useButtonWasJustPressed = false, destroyButtonWasJustPressed = false;
    private int breakingPlacingSize = 4;

    private final Player player;
    private final Camera camera;
    private final WindowManager window;
}
