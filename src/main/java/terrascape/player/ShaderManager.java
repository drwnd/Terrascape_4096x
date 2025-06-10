package terrascape.player;

import org.joml.*;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.util.HashMap;

public final class ShaderManager {

    public ShaderManager(String vertexShaderFilePath, String fragmentShaderFilePath) throws Exception {
        uniforms = new HashMap<>();
        this.vertexShaderFilePath = vertexShaderFilePath;
        this.fragmentShaderFilePath = fragmentShaderFilePath;

        load();
    }


    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL46.glUniformMatrix4fv(uniforms.get(uniformName), false, value.get(stack.mallocFloat(16)));
        }
    }

    public void setUniform(String uniformName, int[] data) {
        GL46.glUniform1iv(uniforms.get(uniformName), data);
    }

    public void setUniform(String uniformName, int x, int y) {
        GL46.glUniform2i(uniforms.get(uniformName), x, y);
    }

    public void setUniform(String uniformName, int x, int y, int z) {
        GL46.glUniform3i(uniforms.get(uniformName), x, y, z);
    }

    public void setUniform(String uniformName, int x, int y, int z, int w) {
        GL46.glUniform4i(uniforms.get(uniformName), x, y, z, w);
    }

    public void setUniform(String uniformName, float x, float y, float z) {
        GL46.glUniform3f(uniforms.get(uniformName), x, y, z);
    }

    public void setUniform(String uniformName, float value) {
        GL46.glUniform1f(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, Vector2f value) {
        GL46.glUniform2f(uniforms.get(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, Vector3f value) {
        GL46.glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, int value) {
        GL46.glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, Color color) {
        GL46.glUniform3f(uniforms.get(uniformName), color.getRed() * 0.003921569f, color.getGreen() * 0.003921569f, color.getBlue() * 0.003921569f);
    }


    public void reload() {
        try {
            int newProgramID = createProgram();
            int newVertexShaderID = createVertexShader(ObjectLoader.loadResources(vertexShaderFilePath), newProgramID);
            int newFragmentShaderID = createFragmentShader(ObjectLoader.loadResources(fragmentShaderFilePath), newProgramID);

            link(newProgramID, newVertexShaderID, newFragmentShaderID);

            cleanUp();

            programID = newProgramID;
            vertexShaderID = newVertexShaderID;
            fragmentShaderID = newFragmentShaderID;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        HashMap<String, Integer> oldUniforms = uniforms;
        uniforms = new HashMap<>(oldUniforms.size());
        for (String uniformName : oldUniforms.keySet()) createUniform(uniformName);
    }

    public void bind() {
        GL46.glUseProgram(programID);
    }

    public void cleanUp() {
        if (programID != 0) GL46.glDeleteProgram(programID);
    }


    private void createUniform(String uniformName) {
        int uniformLocation = GL46.glGetUniformLocation(programID, uniformName);
        if (uniformLocation == -1) System.err.println("Could not find uniform " + uniformName);
        uniforms.put(uniformName, uniformLocation);
    }

    private void load() throws Exception {
        programID = createProgram();
        vertexShaderID = createVertexShader(ObjectLoader.loadResources(vertexShaderFilePath), programID);
        fragmentShaderID = createFragmentShader(ObjectLoader.loadResources(fragmentShaderFilePath), programID);
        link(programID, vertexShaderID, fragmentShaderID);
    }


    public static ShaderManager createOpaqueMaterialShader() throws Exception {
        ShaderManager opaqueMaterialShader = new ShaderManager("shaders/materialVertex.glsl", "shaders/opaqueMaterialFragment.glsl");
        opaqueMaterialShader.createUniform("projectionViewMatrix");
        opaqueMaterialShader.createUniform("iCameraPosition");
        opaqueMaterialShader.createUniform("worldPos");

        opaqueMaterialShader.createUniform("textureAtlas");
        opaqueMaterialShader.createUniform("propertiesTexture");
        return opaqueMaterialShader;
    }

    public static ShaderManager createTransparentMaterialShader() throws Exception {
        ShaderManager transparentMaterialShader = new ShaderManager("shaders/materialVertex.glsl", "shaders/transparentMaterialFragment.glsl");
        transparentMaterialShader.createUniform("projectionViewMatrix");
        transparentMaterialShader.createUniform("worldPos");
        transparentMaterialShader.createUniform("iCameraPosition");

        transparentMaterialShader.createUniform("textureAtlas");
        transparentMaterialShader.createUniform("indexOffset");
        return transparentMaterialShader;
    }

    public static ShaderManager createWaterMaterialShader() throws Exception {
        ShaderManager waterMaterialShader = new ShaderManager("shaders/materialVertex.glsl", "shaders/waterMaterialFragment.glsl");
        waterMaterialShader.createUniform("projectionViewMatrix");
        waterMaterialShader.createUniform("worldPos");
        waterMaterialShader.createUniform("iCameraPosition");

        waterMaterialShader.createUniform("textureAtlas");
        waterMaterialShader.createUniform("time");
        waterMaterialShader.createUniform("flags");
        waterMaterialShader.createUniform("cameraPosition");
        waterMaterialShader.createUniform("sunDirection");
        return waterMaterialShader;
    }


    public static ShaderManager createSkyBoxShader() throws Exception {
        ShaderManager skyBoxShader = new ShaderManager("shaders/skyBoxVertex.glsl", "shaders/skyBoxFragment.glsl");
        skyBoxShader.createUniform("projectionViewMatrix");

        skyBoxShader.createUniform("time");

        skyBoxShader.createUniform("textureAtlas1");
        skyBoxShader.createUniform("textureAtlas2");
        return skyBoxShader;
    }

    public static ShaderManager createTextShader() throws Exception {
        ShaderManager textShader = new ShaderManager("shaders/textVertex.glsl", "shaders/textFragment.glsl");
        textShader.createUniform("screenSize");
        textShader.createUniform("charSize");
        textShader.createUniform("string");
        textShader.createUniform("yOffset");
        textShader.createUniform("xOffset");

        textShader.createUniform("textureAtlas");
        textShader.createUniform("color");
        return textShader;
    }

    public static ShaderManager createGUIShader() throws Exception {
        ShaderManager GUIShader = new ShaderManager("shaders/GUIVertex.glsl", "shaders/GUIFragment.glsl");
        GUIShader.createUniform("position");

        GUIShader.createUniform("textureAtlas");
        return GUIShader;
    }


    public static ShaderManager createSSAOShader() throws Exception {
        ShaderManager ssaoShader = new ShaderManager("shaders/GUIVertex.glsl", "shaders/ssaoFragment.glsl");
        ssaoShader.createUniform("position");

        ssaoShader.createUniform("depthTexture");
        ssaoShader.createUniform("noiseTexture");
        ssaoShader.createUniform("projectionMatrix");
        ssaoShader.createUniform("projectionInverse");
        ssaoShader.createUniform("noiseScale");
        return ssaoShader;
    }

    public static ShaderManager createPostShader() throws Exception {
        ShaderManager postShader = new ShaderManager("shaders/GUIVertex.glsl", "shaders/postFragment.glsl");
        postShader.createUniform("position");

        postShader.createUniform("normalTexture");
        postShader.createUniform("positionTexture");
        postShader.createUniform("colorTexture");
        postShader.createUniform("ssaoTexture");
        postShader.createUniform("lightTexture");
        postShader.createUniform("propertiesTexture");
        postShader.createUniform("screenSize");
        postShader.createUniform("flags");
        postShader.createUniform("time");
        postShader.createUniform("cameraPosition");
        postShader.createUniform("sunDirection");
        return postShader;
    }

    public static ShaderManager createLightShader() throws Exception {
        ShaderManager lightShader = new ShaderManager("shaders/lightVertex.glsl", "shaders/lightFragment.glsl");
        lightShader.createUniform("projectionViewMatrix");
        lightShader.createUniform("iCameraPosition");
        lightShader.createUniform("worldPos");

        lightShader.createUniform("normalTexture");
        lightShader.createUniform("positionTexture");
        lightShader.createUniform("screenSize");
        return lightShader;
    }

    public static ShaderManager createLightPrePassShader() throws Exception {
        ShaderManager lightPrePassShader = new ShaderManager("shaders/lightVertex.glsl", "shaders/nullFragment.glsl");
        lightPrePassShader.createUniform("projectionViewMatrix");
        lightPrePassShader.createUniform("iCameraPosition");
        lightPrePassShader.createUniform("worldPos");
        return lightPrePassShader;
    }


    public static ShaderManager createOpaqueParticleShader() throws Exception {
        ShaderManager opaqueParticleShader = new ShaderManager("shaders/particleVertex.glsl", "shaders/opaqueMaterialFragment.glsl");
        opaqueParticleShader.createUniform("projectionViewMatrix");
        opaqueParticleShader.createUniform("iCameraPosition");
        opaqueParticleShader.createUniform("spawnTime");
        opaqueParticleShader.createUniform("currentTime");

        opaqueParticleShader.createUniform("textureAtlas");
        opaqueParticleShader.createUniform("propertiesTexture");

        return opaqueParticleShader;
    }

    public static ShaderManager createTransparentParticleShader() throws Exception {
        ShaderManager transparentParticleShader = new ShaderManager("shaders/particleVertex.glsl", "shaders/transparentMaterialFragment.glsl");
        transparentParticleShader.createUniform("projectionViewMatrix");
        transparentParticleShader.createUniform("iCameraPosition");
        transparentParticleShader.createUniform("spawnTime");

        transparentParticleShader.createUniform("textureAtlas");
        transparentParticleShader.createUniform("currentTime");

        return transparentParticleShader;
    }


    private static int createProgram() throws Exception {
        int programID = GL46.glCreateProgram();
        if (programID == 0) throw new Exception("Could not create Shader");
        return programID;
    }

    private static int createVertexShader(String shaderCode, int programID) throws Exception {
        return createShader(shaderCode, GL46.GL_VERTEX_SHADER, programID);
    }

    private static int createFragmentShader(String shaderCode, int programID) throws Exception {
        return createShader(shaderCode, GL46.GL_FRAGMENT_SHADER, programID);
    }

    private static void link(int programID, int vertexShaderID, int fragmentShaderID) throws Exception {
        GL46.glLinkProgram(programID);

        if (GL46.glGetProgrami(programID, GL46.GL_LINK_STATUS) == 0)
            throw new Exception("Error linking shader code: " + GL46.glGetProgramInfoLog(programID, 1024));

        if (vertexShaderID != 0) GL46.glDetachShader(programID, vertexShaderID);
        if (fragmentShaderID != 0) GL46.glDetachShader(programID, fragmentShaderID);

        GL46.glValidateProgram(programID);
        if (GL46.glGetProgrami(programID, GL46.GL_VALIDATE_STATUS) == 0)
            throw new Exception("Unable to validate shader code: " + GL46.glGetProgramInfoLog(programID, 1024));
    }

    private static int createShader(String shaderCode, int shaderType, int programID) throws Exception {
        int shaderID = GL46.glCreateShader(shaderType);
        if (shaderID == 0) throw new Exception("Error creating shader. Type: " + shaderType);

        GL46.glShaderSource(shaderID, shaderCode);
        GL46.glCompileShader(shaderID);

        if (GL46.glGetShaderi(shaderID, GL46.GL_COMPILE_STATUS) == 0)
            throw new Exception("Error compiling shader code: Type: " + shaderType + "Info: " + GL46.glGetShaderInfoLog(shaderID, 1024));

        GL46.glAttachShader(programID, shaderID);

        return shaderID;
    }


    private final String vertexShaderFilePath, fragmentShaderFilePath;

    private int programID, vertexShaderID, fragmentShaderID;
    private HashMap<String, Integer> uniforms;
}
