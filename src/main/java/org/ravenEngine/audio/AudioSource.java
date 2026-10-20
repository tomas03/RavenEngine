package org.ravenEngine.audio;

import org.ravenEngine.components.Component;
import org.ravenEngine.util.AssetPool;

public class AudioSource extends Component {
    private String filepath;
    private boolean loop;
    private float volume = 0.5f;
    private boolean playOnStart = true;

    private transient Sound sound;
    private transient boolean startedPlaying = false;

    public AudioSource(String filepath, boolean loop, boolean playOnStart, float volume) {
        this.filepath = filepath;
        this.loop = loop;
        this.playOnStart = playOnStart;
        this.volume = volume;
    }

    public AudioSource() {
        this.filepath = "";
        this.loop = false;
        this.playOnStart = false;
        this.volume = 1.0f;
    }

    @Override
    public void start() {
        this.sound = AssetPool.addSound(filepath, loop);
        this.sound.setVolume(volume);
    }

    @Override
    public void update(float dt) {
        // update() SOLO corre cuando Engine.IsPlaying() es true
        if (playOnStart && !startedPlaying && sound != null) {
            sound.play();
            startedPlaying = true;
        }
    }

    public void play() {
        if (sound != null) {
            sound.play();
        }
    }

    public void stop() {
        if (sound != null) {
            sound.stop();
            startedPlaying = false;
        }
    }

    public void pause() {
        if (sound != null) {
            sound.pause();
        }
    }

    public Sound getSound() {
        return sound;
    }
}