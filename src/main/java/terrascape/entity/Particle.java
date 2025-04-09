package terrascape.entity;

import terrascape.player.RenderManager;

import java.util.ArrayList;

import static terrascape.utils.Constants.*;

public record Particle(int x, int y, int z, byte material, long spawnTime, float lifeTime,
                       float rotationSpeedJaw, float rotationSpeedPitch, float gravity,
                       float velocityX, float velocityY, float velocityZ) {

    public static void update() {
        long currentTime = System.nanoTime();
        synchronized (particles) {
            particles.removeIf(particle -> particle.spawnTime + particle.lifeTime * NANOSECONDS_PER_SECOND < currentTime);
        }
    }

    public static void renderParticles(RenderManager renderer) {
        synchronized (particles) {
            for (Particle particle : particles) renderer.processParticle(particle);
        }
    }

    public static void addBreakParticle(int x, int y, int z, byte material) {
        synchronized (particles) {
            particles.add(new Particle(x, y, z, material, System.nanoTime(), BREAK_PARTICLE_LIFETIME,
                    getRandom(0.0f, 5f), getRandom(0.0f, 5f), BREAK_PARTICLE_GRAVITY,
                    getRandom(-12f, 12f), getRandom(-2f, 25f), getRandom(-12f, 12f)));
        }
    }

    private static float getRandom(float min, float max) {
        return (float) Math.random() * (max - min) + min;
    }

    private static final ArrayList<Particle> particles = new ArrayList<>();

    private static final float BREAK_PARTICLE_LIFETIME = 2;
    private static final float BREAK_PARTICLE_GRAVITY = 80;
}
