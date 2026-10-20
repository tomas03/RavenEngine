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
    private final STBTTBakedChar.Buffer cdata = STBTTBakedChar.malloc(96); // ASCII 32 a 127
    private final int bitmapWidth = 1024;
    private final int bitmapHeight = 1024;
    private float fontSize = 24.0f;
    private String fontPath;

    public Font(String fontPath, float fontSize) {
        this.fontPath = fontPath;
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

            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            ByteBuffer ttfBuffer = BufferUtils.createByteBuffer((int) fc.size());
            fc.read(ttfBuffer);
            fc.close();
            fis.close();
            ttfBuffer.flip();

            ByteBuffer bitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);
            int result = STBTruetype.stbtt_BakeFontBitmap(ttfBuffer, fontSize, bitmap, bitmapWidth, bitmapHeight, 32, cdata);

            if (result <= 0) {
                System.err.println("[Font WARN] No entraron todos los caracteres en el atlas para tamaño: " + fontSize);
            }

            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, bitmapWidth, bitmapHeight, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glBindTexture(GL_TEXTURE_2D, 0);

        } catch (IOException e) {
            System.err.println("[Font ERROR] Error al cargar la fuente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void destroy() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
        cdata.free();
    }

    public int getTextureId() { return textureId; }
    public STBTTBakedChar.Buffer getCdata() { return cdata; }
    public int getBitmapWidth() { return bitmapWidth; }
    public int getBitmapHeight() { return bitmapHeight; }
    public float getFontSize() { return fontSize; }
    public String getFontPath() { return fontPath; }
}