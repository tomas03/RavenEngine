package org.ravenEngine.audio;

import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCStdlib;

import java.io.File;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;

public class Sound {
    private int bufferId;
    private int sourceId;
    private final String filepath;
    private boolean isPlaying = false;

    public Sound(String filepath, boolean loops) {
        this.filepath = filepath;

        File file = new File(filepath);
        if (!file.exists()) {
            System.err.println("[Audio ERROR] No se encontró el archivo de audio: " + filepath);
            return;
        }

        // 1. Decodificar el archivo OGG a PCM crudo usando STB Vorbis
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channelsBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);

            ShortBuffer rawAudio = STBVorbis.stb_vorbis_decode_filename(
                    file.getAbsolutePath(),
                    channelsBuffer,
                    sampleRateBuffer
            );

            if (rawAudio == null) {
                System.err.println("[Audio ERROR] Falló la decodificación de: " + filepath);
                return;
            }

            int channels = channelsBuffer.get();
            int sampleRate = sampleRateBuffer.get();

            // Determinar formato mono o estéreo (16 bits)
            int format = -1;
            if (channels == 1) {
                format = AL_FORMAT_MONO16;
            } else if (channels == 2) {
                format = AL_FORMAT_STEREO16;
            }

            // 2. Generar el buffer y cargar los datos PCM
            this.bufferId = alGenBuffers();
            alBufferData(bufferId, format, rawAudio, sampleRate);

            // 3. Generar la fuente (source) de sonido
            this.sourceId = alGenSources();
            alSourcei(sourceId, AL_BUFFER, bufferId);
            alSourcei(sourceId, AL_LOOPING, loops ? AL_TRUE : AL_FALSE);
            alSourcei(sourceId, AL_POSITION, 0);
            alSourcef(sourceId, AL_GAIN, 0.5f); // Volumen al 50% por defecto

            // Liberar la memoria nativa asignada por STB
            LibCStdlib.free(rawAudio);
        }
    }
    public void play() {
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) {
            isPlaying = false;
            alSourcei(sourceId, AL_POSITION, 0);
        }

        if (!isPlaying) {
            alSourcePlay(sourceId);
            isPlaying = true;
        }
    }
    public void pause() {
        if (isPlaying) {
            alSourcePause(sourceId);
            isPlaying = false;
        }
    }
    public void stop() {
        if (isPlaying) {
            alSourceStop(sourceId);
            isPlaying = false;
        }
    }

    public void setVolume(float volume) {
        // Rango de 0.0f a 1.0f (o más para amplificación)
        alSourcef(sourceId, AL_GAIN, Math.max(0.0f, volume));
    }

    public void setLooping(boolean loop) {
        alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
    }

    public boolean isPlaying() {
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        isPlaying = (state == AL_PLAYING);
        return isPlaying;
    }

    public void destroy() {
        stop();
        alDeleteSources(sourceId);
        alDeleteBuffers(bufferId);
    }

    public String getFilepath() {
        return filepath;
    }
}
