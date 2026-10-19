package org.ravenEngine.renderer.text;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class Font {

    private int textureId;
    private final STBTTBakedChar.Buffer cdata = STBTTBakedChar.malloc(96); // Caracteres ASCII 32 a 127
    private final int bitmapWidth = 512;
    private final int bitmapHeight = 512;
    private float fontSize = 24.0f;

    public Font(String fontPath, float fontSize) {
        this.fontSize = fontSize;
        loadFont(fontPath);
    }

    private void loadFont(String fontPath) {
        try {
            File file = new File(fontPath);
            if (!file.exists()) {
                System.err.println("[Font ERROR] No se encontró el archivo TTF: " + fontPath);
                return;
            }

            // Lectura del archivo a ByteBuffer directo
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            ByteBuffer ttfBuffer = BufferUtils.createByteBuffer((int) fc.size());
            fc.read(ttfBuffer);
            fc.close();
            fis.close();
            ttfBuffer.flip();

            // Horneado del bitmap de glifos en memoria
            ByteBuffer bitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);
            STBTruetype.stbtt_BakeFontBitmap(ttfBuffer, fontSize, bitmap, bitmapWidth, bitmapHeight, 32, cdata);

            // Subir atlas a textura de OpenGL
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, bitmapWidth, bitmapHeight, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glBindTexture(GL_TEXTURE_2D, 0);
            System.out.println("[Font] Fuente cargada correctamente: " + fontPath);

        } catch (IOException e) {
            System.err.println("[Font ERROR] Error al cargar la fuente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getTextureId() {
        return textureId;
    }

    public STBTTBakedChar.Buffer getCdata() {
        return cdata;
    }

    public int getBitmapWidth() {
        return bitmapWidth;
    }

    public int getBitmapHeight() {
        return bitmapHeight;
    }

    public float getFontSize() {
        return fontSize;
    }
}