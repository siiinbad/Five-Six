package main;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.*;

public class AudioManager {

    public boolean isMuted     = false;
    public float   musicVolume = 0.25f;
    public float   sfxVolume   = 0.5f;

    private String   musicTrack          = "";
    private Thread   musicThread         = null;
    private volatile boolean stopMusic   = false;
    private volatile Clip    activeClip  = null;

    // ── Music ─────────────────────────────────────────────────────
    public void playMusic(String name) {
        if (name == null || name.isBlank()) return;
        if (name.equals(musicTrack) && musicThread != null && musicThread.isAlive()) return;
        stopMusic();
        musicTrack = name;
        if (isMuted) return;
        stopMusic = false;
        musicThread = new Thread(() -> {
            while (!stopMusic) {
                try {
                    InputStream raw = openStream(name);
                    if (raw == null) return;
                    AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    setGain(clip, musicVolume);
                    activeClip = clip;
                    clip.start();
                    while (!stopMusic) {
                        Thread.sleep(100);
                        if (!clip.isRunning() && !clip.isActive()) break;
                    }
                    clip.stop();
                    clip.close();
                    activeClip = null;
                } catch (Exception e) {
                    activeClip = null;
                    return;
                }
            }
        }, "music-" + name);
        musicThread.setDaemon(true);
        musicThread.start();
    }

    public void stopMusic() {
        stopMusic = true;
        Clip c = activeClip;
        if (c != null) { try { c.stop(); c.close(); } catch (Exception ignored) {} }
        Thread t = musicThread;
        if (t != null && t.isAlive()) {
            try { t.join(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            if (t.isAlive()) t.interrupt();
        }
        musicThread = null;
        activeClip  = null;
    }

    public void applyMusicVolume() {
        if (isMuted) return;
        Clip c = activeClip;
        if (c != null) setGain(c, musicVolume);
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            stopMusic();
        } else {
            String track = musicTrack;
            musicTrack = "";
            playMusic(track);
        }
    }

    // ── SFX ───────────────────────────────────────────────────────
    public void playSFX(String name) {
        if (isMuted || sfxVolume <= 0f) return;
        Thread t = new Thread(() -> {
            try {
                InputStream raw = AudioManager.class.getResourceAsStream("/res/soundtrack/" + name + ".wav");
                if (raw == null) return;
                AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                setGain(clip, sfxVolume);
                clip.start();
                while (!clip.isRunning()) Thread.sleep(10);
                while (clip.isRunning())  Thread.sleep(10);
                clip.close();
            } catch (Exception ignored) {}
        }, "sfx-" + name);
        t.setDaemon(true);
        t.start();
    }

    // ── Debug ─────────────────────────────────────────────────────
    public void debugAudio() {
        InputStream ms = openStream("menu_sountrack");
        System.out.println("Music stream: " + (ms != null ? "FOUND" : "NOT FOUND"));
        InputStream ss = AudioManager.class.getResourceAsStream("/res/soundtrack/click.wav");
        System.out.println("SFX click.wav: " + (ss != null ? "FOUND" : "NOT FOUND"));
        try {
            if (ss != null) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(ss));
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                System.out.println("Clip opened OK, frames: " + clip.getFrameLength());
                clip.close();
            }
        } catch (Exception e) {
            System.out.println("Clip error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    private void setGain(Clip clip, float volume) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float)(Math.log10(Math.max(0.0001, volume)) * 20.0);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
        }
    }

    InputStream openStream(String name) {
        String[] paths = {
            "/res/soundtrack/"  + name + ".wav",
            "/res/soundTrack/"  + name + ".wav",
            "/res/soundtrack/"  + name + ".mp3",
            "/res/soundTrack/"  + name + ".mp3"
        };
        for (String path : paths) {
            InputStream s = AudioManager.class.getResourceAsStream(path);
            if (s != null) return s;
        }
        return null;
    }
}
