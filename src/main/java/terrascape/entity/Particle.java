package terrascape.entity;

import terrascape.player.RenderManager;
import terrascape.server.Material;
import terrascape.utils.Utils;

import java.util.ArrayList;

import static terrascape.utils.Constants.*;

public record Particle(int x, int y, int z, int packedVelocityGravity, int packedLifeTimeRotationMaterial,
                       long spawnTime) {

    public static final int SHADER_PARTICLE_INT_SIZE = 6;

    private Particle(int x, int y, int z, byte material, long spawnTime, int lifeTime,
                     float rotationSpeedX, float rotationSpeedY, float gravity,
                     float velocityX, float velocityY, float velocityZ) {
        this(x, y, z,
                packVelocityGravity(velocityX, velocityY, velocityZ, gravity),
                packedLifeTimeRotationMaterial(lifeTime, rotationSpeedX, rotationSpeedY, material),
                spawnTime);
    }

    public static void update() {
        long currentTime = System.nanoTime();
        synchronized (particles) {
            particles.removeIf(particle -> particle.spawnTime + particle.getLifeTimeTicks() * (NANOSECONDS_PER_SECOND / TARGET_TPS) < currentTime);
        }
    }

    public static void renderParticles(RenderManager renderer, long[] visibleChunks) {
        synchronized (particles) {
            for (Particle particle : particles) {
                int chunkIndex = Utils.getChunkIndex(particle.x >> CHUNK_SIZE_BITS, particle.y >> CHUNK_SIZE_BITS, particle.z >> CHUNK_SIZE_BITS);
                if ((visibleChunks[chunkIndex >> 6] & 1L << (chunkIndex & 63)) == 0) continue;
                renderer.processParticle(particle);
            }
        }
    }

    public static void addBreakParticle(int x, int y, int z, byte material) {
        synchronized (particles) {
            particles.add(new Particle(x, y, z, Material.getTextureIndex(material), System.nanoTime(), BREAK_PARTICLE_LIFETIME_TICKS,
                    getRandom(0.0f, 5f), getRandom(0.0f, 5f), BREAK_PARTICLE_GRAVITY,
                    getRandom(-12f, 12f), getRandom(-2f, 25f), getRandom(-12f, 12f)));
        }
    }

    private int getLifeTimeTicks() {
        return packedLifeTimeRotationMaterial >> 24 & 0xFF;
    }

    private static float getRandom(float min, float max) {
        return (float) Math.random() * (max - min) + min;
    }

    private static int packVelocityGravity(float velocityX, float velocityY, float velocityZ, float gravity) {
        velocityX = Math.clamp(velocityX, -31.99f, 31.99f);
        velocityY = Math.clamp(velocityY, -31.99f, 31.99f);
        velocityZ = Math.clamp(velocityZ, -31.99f, 31.99f);
        gravity = Math.clamp(gravity, 0.0f, 127.99f);

        int packedVelocityX = (int) (velocityX * VELOCITY_PACKING_FACTOR) + 128 & 0xFF;
        int packedVelocityY = (int) (velocityY * VELOCITY_PACKING_FACTOR) + 128 & 0xFF;
        int packedVelocityZ = (int) (velocityZ * VELOCITY_PACKING_FACTOR) + 128 & 0xFF;
        int packedGravity = (int) (gravity * GRAVITY_PACKING_FACTOR) & 0xFF;

        return packedVelocityX << 24 | packedVelocityY << 16 | packedVelocityZ << 8 | packedGravity;
    }

    private static int packedLifeTimeRotationMaterial(int lifeTime, float rotationSpeedX, float rotationSpeedY, byte material) {
        rotationSpeedX = Math.clamp(rotationSpeedX, 0.0f, 15.99f);
        rotationSpeedY = Math.clamp(rotationSpeedY, 0.0f, 15.99f);

        int packedRotationSpeedX = (int) (rotationSpeedX * ROTATION_PACKING_FACTOR) & 0xFF;
        int packedRotationSpeedY = (int) (rotationSpeedY * ROTATION_PACKING_FACTOR) & 0xFF;

        return lifeTime << 24 | packedRotationSpeedX << 16 | packedRotationSpeedY << 8 | material & 0xFF;
    }

    private static final ArrayList<Particle> particles = new ArrayList<>();

    private static final int BREAK_PARTICLE_LIFETIME_TICKS = 40;
    private static final float BREAK_PARTICLE_GRAVITY = 80;

    private static final float VELOCITY_PACKING_FACTOR = 4.0f; // Inverse in particleVertex.glsl
    private static final float GRAVITY_PACKING_FACTOR = 2.0f; // Inverse in particleVertex.glsl
    private static final float ROTATION_PACKING_FACTOR = 16.0f; // Inverse in particleVertex.glsl
}
