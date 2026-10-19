package org.ravenEngine.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class AudioManager {
    private static long audioDevice = NULL;
    private static long audioContext = NULL;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

        // 1. Abrir el dispositivo de salida por defecto
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(defaultDeviceName);
        if (audioDevice == NULL) {
            System.err.println("[AudioManager ERROR] No se pudo inicializar el dispositivo de audio");
            return;
        }

        // 2. Crear y activar el contexto
        int[] attributes = { 0 };
        audioContext = alcCreateContext(audioDevice, attributes);
        alcMakeContextCurrent(audioContext);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        if (!alCapabilities.OpenAL10) {
            System.err.println("[AudioManager ERROR] OpenAL 1.0 no soportado en este dispositivo");
            return;
        }

        // 3. Configurar al oyente (Listener) mirando al plano 2D
        alListener3f(AL_POSITION, 0.0f, 0.0f, 0.0f);
        alListener3f(AL_VELOCITY, 0.0f, 0.0f, 0.0f);

        initialized = true;
        System.out.println("[AudioManager] Subsistema de sonido OpenAL iniciado correctamente");
    }

    public static void setListenerPosition(float x, float y) {
        if (initialized) {
            alListener3f(AL_POSITION, x, y, 0.0f);
        }
    }

    public static void cleanup() {
        if (!initialized) return;

        alcMakeContextCurrent(NULL);
        if (audioContext != NULL) {
            alcDestroyContext(audioContext);
        }
        if (audioDevice != NULL) {
            alcCloseDevice(audioDevice);
        }

        initialized = false;
        System.out.println("[AudioManager] Subsistema de sonido cerrado");
    }
}