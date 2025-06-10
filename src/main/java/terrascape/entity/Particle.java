package terrascape.entity;

import terrascape.server.Material;

public record Particle(int x, int y, int z, int packedVelocityGravity, int packedLifeTimeRotationTexture,byte material) {

    Particle(int x, int y, int z, byte material, int lifeTimeTicks,
             float rotationSpeedX, float rotationSpeedY, float gravity,
             float velocityX, float velocityY, float velocityZ) {
        this(x, y, z,
                packVelocityGravity(velocityX, velocityY, velocityZ, gravity),
                packLifeTimeRotationTexture(lifeTimeTicks, rotationSpeedX, rotationSpeedY, material),
                material);
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

    private static int packLifeTimeRotationTexture(int lifeTime, float rotationSpeedX, float rotationSpeedY, byte material) {
        lifeTime = Math.clamp(lifeTime, 0, 255);
        rotationSpeedX = Math.clamp(rotationSpeedX, 0.0f, 15.99f);
        rotationSpeedY = Math.clamp(rotationSpeedY, 0.0f, 15.99f);

        int packedRotationSpeedX = (int) (rotationSpeedX * ROTATION_PACKING_FACTOR) & 0xFF;
        int packedRotationSpeedY = (int) (rotationSpeedY * ROTATION_PACKING_FACTOR) & 0xFF;

        return lifeTime << 24 | packedRotationSpeedX << 16 | packedRotationSpeedY << 8 | Material.getTextureIndex(material) & 0xFF;
    }

    private static final float VELOCITY_PACKING_FACTOR = 4.0f; // Inverse in particleVertex.glsl
    private static final float GRAVITY_PACKING_FACTOR = 2.0f; // Inverse in particleVertex.glsl
    private static final float ROTATION_PACKING_FACTOR = 16.0f; // Inverse in particleVertex.glsl
}
