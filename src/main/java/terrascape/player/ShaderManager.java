package terrascape.player;

import org.joml.*;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public final class ShaderManager {

    public ShaderManager() throws Exception {
        programID = GL46.glCreateProgram();
        if (programID == 0) throw new Exception("Could not create Shader");

        uniforms = new HashMap<>();
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

    public void setUniform(String uniformName, int x, int y, int z, int w) {
        GL46.glUniform4i(uniforms.get(uniformName), x, y, z, w);
    }

    public void setUniform(String uniformName, Vector3f value) {
        GL46.glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, float value) {
        GL46.glUniform1f(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, Vector2f value) {
        GL46.glUniform2f(uniforms.get(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, int value) {
        GL46.glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, Color color) {
        GL46.glUniform3f(uniforms.get(uniformName), color.getRed() * 0.003921569f, color.getGreen() * 0.003921569f, color.getBlue() * 0.003921569f);
    }


    public void createUniform(String uniformName) {
        int uniformLocation = GL46.glGetUniformLocation(programID, uniformName);
        if (uniformLocation < 0) System.err.println("Could not find uniform " + uniformName);
        uniforms.put(uniformName, uniformLocation);
    }

    public void createVertexShader(String shaderCode) throws Exception {
        vertexShaderID = createShader(shaderCode, GL46.GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderID = createShader(shaderCode, GL46.GL_FRAGMENT_SHADER);
    }


    public void link() throws Exception {
        GL46.glLinkProgram(programID);

        if (GL46.glGetProgrami(programID, GL46.GL_LINK_STATUS) == 0)
            throw new Exception("Error linking shader code: " + GL46.glGetProgramInfoLog(programID, 1024));

        if (vertexShaderID != 0) GL46.glDetachShader(programID, vertexShaderID);
        if (fragmentShaderID != 0) GL46.glDetachShader(programID, fragmentShaderID);

        GL46.glValidateProgram(programID);
        if (GL46.glGetProgrami(programID, GL46.GL_VALIDATE_STATUS) == 0)
            throw new Exception("Unable to validate shader code: " + GL46.glGetProgramInfoLog(programID, 1024));
    }

    public void bind() {
        GL46.glUseProgram(programID);
    }

    public void unBind() {
        GL46.glUseProgram(0);
    }

    public void cleanUp() {
        unBind();
        if (programID != 0) GL46.glDeleteProgram(programID);
    }


    private int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderID = GL46.glCreateShader(shaderType);
        if (shaderID == 0)
            throw new Exception("Error creating shader. Type: " + shaderType);

        GL46.glShaderSource(shaderID, shaderCode);
        GL46.glCompileShader(shaderID);

        if (GL46.glGetShaderi(shaderID, GL46.GL_COMPILE_STATUS) == 0)
            throw new Exception("Error compiling shader code: Type: " + shaderType + "Info: " + GL46.glGetShaderInfoLog(shaderID, 1024));

        GL46.glAttachShader(programID, shaderID);

        return shaderID;
    }

    private final int programID;
    private int vertexShaderID, fragmentShaderID;

    private final Map<String, Integer> uniforms;
}
