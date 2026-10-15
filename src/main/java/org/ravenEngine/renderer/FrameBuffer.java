package org.ravenEngine.renderer;

import static org.lwjgl.opengl.GL30.*;

public class FrameBuffer {
    private int fboId = 0;
    private int textureId = 0;
    private int rboId = 0;
    private int width;
    private int height;

    public FrameBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        init();
    }

    public void init(){
        // 1. Generar el Framebuffer Object (FBO)
        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        // 2. Crear la textura donde se va a pintar la escena
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        // Filtrado NEAREST para que el pixel art se mantenga nítido
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        // 3. Crear el Renderbuffer para Depth y Stencil
        rboId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rboId);

        // 4. Validar que la GPU haya creado el FBO correctamente
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("[Framebuffer] Error: El Framebuffer no está completo.");
        }

        // Desenlazar para volver al buffer por defecto
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind(){
        glBindFramebuffer(GL_FRAMEBUFFER,fboId);
        glViewport(0,0,width,height);
    }

    public void unbind(){
        glBindFramebuffer(GL_FRAMEBUFFER,0);
    }
    public void destroy(){
        glDeleteFramebuffers(fboId);
        glDeleteTextures(textureId);
        glDeleteRenderbuffers(rboId);
    }

    public int getTextureId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
