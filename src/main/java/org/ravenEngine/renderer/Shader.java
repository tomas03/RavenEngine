package org.ravenEngine.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private int shaderProgramId;
    private final String vertexPath;
    private final String fragmentPath;

    public Shader(String vertexPath, String fragmentPath) {
        this.vertexPath = vertexPath;
        this.fragmentPath = fragmentPath;
    }

    public void compile() {
        String vertexSource = loadResource(vertexPath);
        String fragmentSource = loadResource(fragmentPath);

        // Compilar Vertex Shader
        int vertexId = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexId, vertexSource);
        glCompileShader(vertexId);
        checkCompileErrors(vertexId, "VERTEX");

        // Compilar Fragment Shader
        int fragmentId = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentId, fragmentSource);
        glCompileShader(fragmentId);
        checkCompileErrors(fragmentId, "FRAGMENT");

        // Enlazar programa
        shaderProgramId = glCreateProgram();
        glAttachShader(shaderProgramId, vertexId);
        glAttachShader(shaderProgramId, fragmentId);
        glLinkProgram(shaderProgramId);
        checkLinkErrors(shaderProgramId);

        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    public void use() {
        glUseProgram(shaderProgramId);
    }

    public void detach() {
        glUseProgram(0);
    }

    // import static org.lwjgl.opengl.GL20.*;

    public void destroy() {
        if (this.shaderProgramId != 0) { // o this.programId
            glDeleteProgram(this.shaderProgramId);
            this.shaderProgramId = 0;
        }
    }

    public void uploadInt(String varName, int value) {
        int varLocation = glGetUniformLocation(shaderProgramId, varName);
        use();
        glUniform1i(varLocation, value);
    }
    public void uploadIntArray(String varName, int[] array) {
        int varLocation = glGetUniformLocation(shaderProgramId, varName);
        use();
        glUniform1iv(varLocation, array);
    }

    private String loadResource(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("No se encontro el archivo de shader: " + path);
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // Saca marcas BOM de UTF-8, saltos de retorno de carro y espacios al principio
            return content.replace("\uFEFF", "")
                    .replace("\r", "")
                    .stripLeading();
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo shader: " + path, e);
        }
    }

    private void checkCompileErrors(int shaderId, String type) {
        int success = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        if (success == 0) {
            int len = glGetShaderi(shaderId, GL_INFO_LOG_LENGTH);
            throw new RuntimeException("Error al compilar " + type + " shader:\n" + glGetShaderInfoLog(shaderId, len));
        }
    }

    private void checkLinkErrors(int programId) {
        int success = glGetProgrami(programId, GL_LINK_STATUS);
        if (success == 0) {
            int len = glGetProgrami(programId, GL_INFO_LOG_LENGTH);
            throw new RuntimeException("Error al linkear Shader Program:\n" + glGetProgramInfoLog(programId, len));
        }
    }

    public void uploadMat4f(String varName, org.joml.Matrix4f mat4) {
        int varLocation = glGetUniformLocation(shaderProgramId, varName);
        use();
        java.nio.FloatBuffer matBuffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
        mat4.get(matBuffer);
        glUniformMatrix4fv(varLocation, false, matBuffer);
    }

}