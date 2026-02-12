package com.libraryplus.util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class AudioManager {
    private static final Logger logger = LoggerFactory.getLogger(AudioManager.class);
    private static AudioManager instance;
    private MediaPlayer mediaPlayer;
    private double volume = 0.5;
    private boolean isMuted = false;

    private AudioManager() {
    }

    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void playMusic() {
        if (mediaPlayer != null) {
            return; 
        }

        try {
            String projectDir = System.getProperty("user.dir");
            File musicFile = new File(projectDir, "data/music/background_music.mp3");

            if (!musicFile.exists()) {
                logger.warn("Music file not found at: " + musicFile.getAbsolutePath());
                return;
            }

            
            long fileSizeInBytes = musicFile.length();
            long fileSizeInMB = fileSizeInBytes / (1024 * 1024);
            if (fileSizeInMB > 30) {
                logger.warn("Music file is too large ({} MB). Limit is 30MB. Playback skipped.", fileSizeInMB);
                System.out.println("Music file is too large (" + fileSizeInMB + " MB). Playback skipped.");
                return;
            }
            Media media = new Media(musicFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnError(() -> {
                logger.error("Media player error: " + mediaPlayer.getError().getMessage(), mediaPlayer.getError());
            });

            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setVolume(volume);
            mediaPlayer.play();
            logger.info("Background music started.");
        } catch (Throwable e) {
            logger.error("Failed to play background music", e);
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0, Math.min(1, volume));
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(this.volume);
        }
    }

    public double getVolume() {
        return volume;
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (mediaPlayer != null) {
            mediaPlayer.setMute(isMuted);
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }
}
