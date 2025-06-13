package terrascape.entity;

import org.lwjgl.opengl.GL46;
import terrascape.player.RenderManager;
import terrascape.server.Chunk;
import terrascape.server.Launcher;
import terrascape.server.Material;
import terrascape.server.ServerLogic;

import java.util.ArrayList;
import java.util.Iterator;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Constants.PARTICLE_TIME_SHIFT;
import static terrascape.utils.Settings.DIG_GAIN;

public record ParticleEffect(int buffer, int spawnTime, int lifeTimeTicks, int count, boolean isOpaque, int x, int y, int z) {

    public static final int SHADER_PARTICLE_INT_SIZE = 3;


    public static void unloadExpiredParticleEffects() {
        int currentTime = (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT);
        synchronized (PARTICLE_EFFECTS) {
            for (Iterator<ParticleEffect> iterator = PARTICLE_EFFECTS.iterator(); iterator.hasNext(); ) {
                ParticleEffect particleEffect = iterator.next();
                if (currentTime - particleEffect.spawnTime() > particleEffect.getLifeTimeNanoSecondsShifted()) {
                    iterator.remove();
                    GL46.glDeleteBuffers(particleEffect.buffer);
                }
            }
        }
    }

    public static void addParticleEffect(ParticleEffect particleEffect) {
        synchronized (PARTICLE_EFFECTS) {
            PARTICLE_EFFECTS.add(particleEffect);
        }
    }

    public static void renderParticleEffects(RenderManager renderer) {
        synchronized (PARTICLE_EFFECTS) {
            for (ParticleEffect particleEffect : PARTICLE_EFFECTS) renderer.processParticleEffect(particleEffect);
        }
    }

    public static void addBreakParticleEffect(int startX, int startY, int startZ, int sideLength, byte ignoreMaterial) {
        ArrayList<Particle> opaqueParticles = new ArrayList<>(sideLength * sideLength);
        ArrayList<Particle> transparentParticles = new ArrayList<>(sideLength * sideLength);

        for (int xOffset = 0; xOffset < sideLength; xOffset++)
            for (int yOffset = 0; yOffset < sideLength; yOffset++)
                for (int zOffset = 0; zOffset < sideLength; zOffset++) {
                    byte particleMaterial = Chunk.getMaterialInWorld(startX + xOffset, startY + yOffset, startZ + zOffset);
                    if (particleMaterial == AIR || particleMaterial == OUT_OF_WORLD || particleMaterial == ignoreMaterial) continue;

                    (Material.isSemiTransparentMaterial(particleMaterial) ? transparentParticles : opaqueParticles)
                            .add(new Particle(xOffset, yOffset, zOffset, particleMaterial, BREAK_PARTICLE_LIFETIME_TICKS,
                                    getRandom(0.0f, 5f), getRandom(0.0f, 5f), BREAK_PARTICLE_GRAVITY,
                                    getRandom(-12f, 12f), getRandom(-2f, 25f), getRandom(-12f, 12f)));
                }

        int spawnTime = (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT);
        if (!opaqueParticles.isEmpty()) {
            int[] particlesData = packParticlesIntoBuffer(opaqueParticles);
            ToBufferParticleEffect effect = new ToBufferParticleEffect(particlesData, spawnTime, BREAK_PARTICLE_LIFETIME_TICKS, true, startX, startY, startZ);
            ServerLogic.addToBufferParticleEffect(effect);
        }
        if (!transparentParticles.isEmpty()) {
            int[] particlesData = packParticlesIntoBuffer(transparentParticles);
            ToBufferParticleEffect effect = new ToBufferParticleEffect(particlesData, spawnTime, BREAK_PARTICLE_LIFETIME_TICKS, false, startX, startY, startZ);
            ServerLogic.addToBufferParticleEffect(effect);
        }
    }

    public static void addSplashParticleEffect(int x, int y, int z, byte material) {
        Launcher.getSound().playRandomSound(Material.getDigSound(material), x, y, z, 0.0f, 0.0f, 0.0f, DIG_GAIN);
        ArrayList<Particle> particles = new ArrayList<>(SPLASH_PARTICLE_COUNT);

        for (int count = 0; count < SPLASH_PARTICLE_COUNT; count++) {
            double angle = (double) 2 * Math.PI * count / SPLASH_PARTICLE_COUNT;

            float velocityX = (float) Math.sin(angle) * 17.0f + getRandom(-3.0f, 3.0f);
            float velocityY = 12.0f + getRandom(-3.0f, 3.0f);
            float velocityZ = (float) Math.cos(angle) * 17.0f + getRandom(-3.0f, 3.0f);

            int xOffset = (int) getRandom(-5.0f, 5.0f);
            int yOffset = (int) getRandom(-5.0f, 5.0f);
            int zOffset = (int) getRandom(-5.0f, 5.0f);

            particles.add(new Particle(xOffset, yOffset, zOffset, material, SPLASH_PARTICLE_LIFETIME_TICKS,
                    getRandom(0.0f, 5f), getRandom(0.0f, 5f), SPLASH_PARTICLE_GRAVITY,
                    velocityX, velocityY, velocityZ));
        }

        int spawnTime = (int) (System.nanoTime() >> PARTICLE_TIME_SHIFT);
        int[] particlesData = packParticlesIntoBuffer(particles);
        ServerLogic.addToBufferParticleEffect(new ToBufferParticleEffect(particlesData, spawnTime, SPLASH_PARTICLE_LIFETIME_TICKS, true, x, y, z));
    }


    private int getLifeTimeNanoSecondsShifted() {
        return (lifeTimeTicks) * ((int) (NANOSECONDS_PER_SECOND / TARGET_TPS) >> PARTICLE_TIME_SHIFT);
    }


    private static float getRandom(float min, float max) {
        return (float) Math.random() * (max - min) + min;
    }

    private static int[] packParticlesIntoBuffer(ArrayList<Particle> particles) {
        int[] particlesData = new int[particles.size() * SHADER_PARTICLE_INT_SIZE];
        int index = 0;

        for (Particle particle : particles) {
            particlesData[index] = particle.packedOffset();
            particlesData[index + 1] = particle.packedVelocityGravity();
            particlesData[index + 2] = particle.packedLifeTimeRotationTexture();

            index += SHADER_PARTICLE_INT_SIZE;
        }

        return particlesData;
    }


    private static final ArrayList<ParticleEffect> PARTICLE_EFFECTS = new ArrayList<>();

    private static final int BREAK_PARTICLE_LIFETIME_TICKS = 40;
    private static final float BREAK_PARTICLE_GRAVITY = 80;

    private static final int SPLASH_PARTICLE_COUNT = 200;
    private static final int SPLASH_PARTICLE_LIFETIME_TICKS = 20;
    private static final float SPLASH_PARTICLE_GRAVITY = 80;

    public record ToBufferParticleEffect(int[] particlesData, int spawnTime, int lifeTimeTicks, boolean isOpaque, int x, int y, int z) {
    }
}
