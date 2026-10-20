package org.ravenEngine.renderer;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.util.AssetPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class DebugDraw {
    private static final int MAX_LINES = 500;
    private static final List<Line2D> lines = new ArrayList<>();

    //6 floats por vértice: 2 pos(x,y) 3 color(r,g,b) 5 floats * 2 vertices
    private static final float[] vertexArray = new float[MAX_LINES*10];
    private static Shader shader;

    private static int vaoID;
    private static int vboID;
    private static boolean started = false;

    public static void start(){
        shader = AssetPool.getShader("/Shaders/debugLine2D.vert", "/Shaders/debugLine2D.frag");

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER,vboID);
        glBufferData(GL_ARRAY_BUFFER,(long)vertexArray.length*Float.BYTES,GL_DYNAMIC_DRAW);

        // Atributo 0: Posición (2 floats)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Atributo 1: Color (3 floats)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 5 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glLineWidth(2.0f);
        glBindVertexArray(0);
        started = true;
    }

    public static void beginFrame(){
        if (!started){
            start();
        }

        //eliminar líneas cuyo lifetime expiró
        for (int i = 0;i<lines.size();i++){
            if (lines.get(i).beginFrame()<0){
                lines.remove(i);
                i--;
            }
        }
    }

    public static void draw(Camera camera) {
        if (!started) {
            start();
        }

        if (lines.isEmpty()) return;

        int index = 0;
        for (Line2D line : lines) {
            for (int i = 0; i < 2; i++) {
                Vector2f position = (i == 0) ? line.getFrom() : line.getTo();
                Vector3f color = line.getColor();

                vertexArray[index]= position.x;
                vertexArray[index + 1] = position.y;
                vertexArray[index + 2] = color.x;
                vertexArray[index + 3] = color.y;
                vertexArray[index + 4] = color.z;
                index += 5;
            }
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, Arrays.copyOfRange(vertexArray, 0, lines.size() * 10));

        shader.use();
        shader.uploadMat4f("uProjection", camera.getProjectionMatrix());
        shader.uploadMat4f("uView", camera.getViewMatrix());

        glBindVertexArray(vaoID);
        glDrawArrays(GL_LINES, 0, lines.size() * 2);

        glBindVertexArray(0);
        shader.detach();
    }

    //MÉTODOS PÚBLICOS DE DIBUJADO
    public static void addLine2D(Vector2f from, Vector2f to, Vector3f color, int lifetime) {
        if (lines.size() >= MAX_LINES) return;
        lines.add(new Line2D(new Vector2f(from), new Vector2f(to), new Vector3f(color), lifetime));
    }

    public static void addLine2D(Vector2f from, Vector2f to, Vector3f color) {
        addLine2D(from, to, color, 1);
    }

    // Dibuja una caja rectangular pasándole coordenadas mínimas y máximas
    public static void addBox2D(Vector2f min, Vector2f max, Vector3f color) {
        Vector2f p1 = new Vector2f(min.x, min.y);
        Vector2f p2 = new Vector2f(max.x, min.y);
        Vector2f p3 = new Vector2f(max.x, max.y);
        Vector2f p4 = new Vector2f(min.x, max.y);

        addLine2D(p1, p2, color);
        addLine2D(p2, p3, color);
        addLine2D(p3, p4, color);
        addLine2D(p4, p1, color);
    }
    // DIBUJA UNA FLECHA CON PUNTA TRIANGULAR PARA REPRESENTAR UN EJE
    public static void addArrow2D(Vector2f from, Vector2f to, Vector3f color, float arrowHeadSize) {
        // Línea principal
        addLine2D(from, to, color);

        // Vector director
        Vector2f dir = new Vector2f(to).sub(from).normalize();
        Vector2f perp = new Vector2f(-dir.y, dir.x);

        // Puntas de la flecha
        Vector2f arrowP1 = new Vector2f(to).sub(new Vector2f(dir).mul(arrowHeadSize)).add(new Vector2f(perp).mul(arrowHeadSize * 0.5f));
        Vector2f arrowP2 = new Vector2f(to).sub(new Vector2f(dir).mul(arrowHeadSize)).sub(new Vector2f(perp).mul(arrowHeadSize * 0.5f));

        addLine2D(to, arrowP1, color);
        addLine2D(to, arrowP2, color);
    }

}
