package terrascape.player;

import org.lwjgl.opengl.GL46;
import terrascape.entity.*;
import org.joml.Vector2f;
import org.joml.Vector3i;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static terrascape.utils.Constants.*;

public final class ObjectLoader {

    public static OpaqueModel loadOpaqueModel(int[] vertices, Vector3i position, int[] vertexCounts, int lod) {
        if (vertices.length == 0) return new OpaqueModel(position, null, 0, lod);
        int vertexBuffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(vertexBuffer, vertices, GL46.GL_STATIC_DRAW);
        return new OpaqueModel(position, vertexCounts, vertexBuffer, lod);
    }

    public static TransparentModel loadTransparentModel(int[] vertices, int waterVertexCount, int glassVertexCount, Vector3i position, int lod) {
        if (waterVertexCount + glassVertexCount == 0) return new TransparentModel(position, 0, 0, 0, lod);
        int vertexBuffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(vertexBuffer, vertices, GL46.GL_STATIC_DRAW);
        return new TransparentModel(position, waterVertexCount, glassVertexCount, vertexBuffer, lod);
    }

    public static LightModel loadLightModel(int[] lights, Vector3i position, int lod) {
        if (lights.length == 0) return null;
        int buffer = GL46.glCreateBuffers();
        GL46.glNamedBufferData(buffer, lights, GL46.GL_STATIC_DRAW);
        return new LightModel(buffer, position, lod, lights.length >> 1);
    }

    public static SkyBox loadSkyBox() throws Exception {
        int vao = createVAO();
        storeIndicesInBuffer(SKY_BOX_INDICES);
        storeDateInAttributeList(0, 3, SKY_BOX_VERTICES);
        storeDateInAttributeList(1, 2, SKY_BOX_TEXTURE_COORDINATES);
        unbind();
        Texture nightTexture = new Texture(loadTexture("textures/706c5e1da58f47ad6e18145165caf55d.png"));
        Texture dayTexture = new Texture(loadTexture("textures/82984-skybox-blue-atmosphere-sky-space-hd-image-free-png.png"));
        return new SkyBox(vao, SKY_BOX_INDICES.length, nightTexture, dayTexture);
    }

    public static GUIElement loadGUIElement(float[] vertices, float[] textureCoordinates, Vector2f position) {
        int vao = createVAO();
        int vbo1 = storeDateInAttributeList(0, 2, vertices);
        int vbo2 = storeDateInAttributeList(1, 2, textureCoordinates);
        unbind();
        return new GUIElement(vao, vertices.length, vbo1, vbo2, position);
    }

    public static int loadTextRow() {
        int vao = createVAO();

        int[] textData = new int[MAX_TEXT_LENGTH * 4];
        for (int i = 0; i < textData.length; i += 4) {
            textData[i] = i >> 2;
            textData[i + 1] = i >> 2 | 128;
            textData[i + 2] = i >> 2 | 256;
            textData[i + 3] = i >> 2 | 384;
        }
        storeDateInAttributeList(textData);
        unbind();

        return vao;
    }

    public static Sphere loadUnitSphere(int rings, int sectors) {
        float R = 1.0f / (float) (rings - 1);
        float S = 1.0f / (float) (sectors - 1);
        int r, s;

        float[] vertices = new float[rings * sectors * 3];
        int index = 0;
        for (r = 0; r < rings; r++)
            for (s = 0; s < sectors; s++) {
                float y = (float) Math.sin(-0.5 * Math.PI + Math.PI * r * R);
                float x = (float) (Math.cos(2 * Math.PI * s * S) * Math.sin(Math.PI * r * R));
                float z = (float) (Math.sin(2 * Math.PI * s * S) * Math.sin(Math.PI * r * R));

                float invLength = 1.0f / (float) Math.sqrt(x * x + y * y + z * z);

                vertices[index++] = x * invLength;
                vertices[index++] = y * invLength;
                vertices[index++] = z * invLength;
            }

        int[] indices = new int[rings * (sectors - 1) * 6];
        index = 0;
        for (r = 0; r < rings; r++)
            for (s = 0; s < sectors - 1; s++) {
                indices[index++] = r * sectors + s;
                indices[index++] = (r + 1) * sectors + s;
                indices[index++] = (r + 1) * sectors + (s + 1);

                indices[index++] = r * sectors + s;
                indices[index++] = (r + 1) * sectors + (s + 1);
                indices[index++] = r * sectors + (s + 1);
            }

        int vao = createVAO();
        storeIndicesInBuffer(indices);
        storeDateInAttributeList(0, 3, vertices);
        unbind();
        return new Sphere(vao, indices.length);
    }

    private static int createVAO() {
        int vao = GL46.glGenVertexArrays();
        GL46.glBindVertexArray(vao);
        return vao;
    }

    private static void storeIndicesInBuffer(int[] indices) {
        int vbo = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, vbo);
        IntBuffer buffer = storeDateInIntBuffer(indices);
        GL46.glBufferData(GL46.GL_ELEMENT_ARRAY_BUFFER, buffer, GL46.GL_STATIC_DRAW);
    }

    private static int storeDateInAttributeList(int attributeNo, int size, float[] data) {
        int vbo = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = storeDateInFloatBuffer(data);
        GL46.glBufferData(GL46.GL_ARRAY_BUFFER, buffer, GL46.GL_STATIC_DRAW);
        GL46.glVertexAttribPointer(attributeNo, size, GL46.GL_FLOAT, false, 0, 0);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    private static void storeDateInAttributeList(int[] data) {
        int vbo = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vbo);
        GL46.glBufferData(GL46.GL_ARRAY_BUFFER, data, GL46.GL_STATIC_DRAW);
        GL46.glVertexAttribIPointer(0, 1, GL46.GL_INT, 0, 0);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, 0);
        // return vbo
    }

    public static FloatBuffer storeDateInFloatBuffer(float[] data) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(data.length);
        buffer.put(data).flip();
        return buffer;
    }

    public static IntBuffer storeDateInIntBuffer(int[] data) {
        IntBuffer buffer = MemoryUtil.memAllocInt(data.length);
        buffer.put(data).flip();
        return buffer;
    }

    public static int loadTexture(String filename) throws Exception {
        int width, height;
        ByteBuffer buffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);

            buffer = STBImage.stbi_load(filename, w, h, c, 4);
            if (buffer == null) throw new Exception("Image FIle " + filename + " not loaded " + STBImage.stbi_failure_reason());

            width = w.get();
            height = h.get();
        }

        int id = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, id);
        GL46.glPixelStorei(GL46.GL_UNPACK_ALIGNMENT, 1);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGBA, width, height, 0, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE, buffer);
        GL46.glGenerateMipmap(GL46.GL_TEXTURE_2D);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        STBImage.stbi_image_free(buffer);
        return id;
    }

    public static int create2DTexture(int internalFormat, int format, int width, int height, int sampling, int type) {
        int texture = GL46.glCreateTextures(GL46.GL_TEXTURE_2D);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, texture);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, 0);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, sampling);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, sampling);
        return texture;
    }

    public static int loadModelIndexBuffer() {
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
        int modelIndexBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, modelIndexBuffer);
        IntBuffer buffer = ObjectLoader.storeDateInIntBuffer(indices);
        GL46.glBufferData(GL46.GL_ELEMENT_ARRAY_BUFFER, buffer, GL46.GL_STATIC_DRAW);

        return modelIndexBuffer;
    }


    private static void unbind() {
        GL46.glBindVertexArray(0);
    }

    //https://ahbejarano.gitbook.io/lwjglgamedev/chapter-16
    public static int loadSound(String filename) {
        int buffer = AL10.alGenBuffers();

        STBVorbisInfo info = STBVorbisInfo.malloc();
        ShortBuffer pcm = readVorbis(filename, info);

        // Copy to buffer
        AL10.alBufferData(buffer, info.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, pcm, info.sample_rate());
//        if (info.channels() != 1) System.out.println(filename);

        return buffer;
    }

    //https://ahbejarano.gitbook.io/lwjglgamedev/chapter-16
    private static ShortBuffer readVorbis(String filename, STBVorbisInfo info) throws RuntimeException {
        MemoryStack stack = MemoryStack.stackPush();
        IntBuffer error = stack.mallocInt(1);
        // IDE has no idea what it's talking about
        long decoder = STBVorbis.stb_vorbis_open_filename(filename, error, null);
        if (decoder == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0));
        }

        STBVorbis.stb_vorbis_get_info(decoder, info);

        int channels = info.channels();

        int lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);

        ShortBuffer result = MemoryUtil.memAllocShort(lengthSamples * channels);

        result.limit(STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, result) * channels);
        STBVorbis.stb_vorbis_close(decoder);

        return result;
    }

    public static String loadResources(String filename) throws Exception {
        String result;

        InputStream in = new FileInputStream(filename);
        Scanner scanner = new Scanner(in, StandardCharsets.UTF_8);
        result = scanner.useDelimiter("\\A").next();

        return result;
    }

    public static void removeVAO(int vao) {
        GL46.glBindVertexArray(vao);
        GL46.glDeleteVertexArrays(vao);
    }

    public static void removeVBO(int vbo) {
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vbo);
        GL46.glDeleteBuffers(vbo);
    }

    public static void cleanUp() {

    }

    private ObjectLoader() {
    }
}
