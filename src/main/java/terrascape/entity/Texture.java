package terrascape.entity;

import terrascape.player.ObjectLoader;

public record Texture(int id) {

    public static final Texture ATLAS;
    public static final Texture PROPERTIES_ATLAS;
    public static final Texture TEXT_ATLAS;

    static {
        try {
            ATLAS = new Texture(ObjectLoader.loadTexture("textures/atlas256.png"));
            PROPERTIES_ATLAS = new Texture(ObjectLoader.loadTexture("textures/properties256.png"));
            TEXT_ATLAS = new Texture(ObjectLoader.loadTexture("textures/textAtlas.png"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
