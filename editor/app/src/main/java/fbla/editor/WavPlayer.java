package fbla.editor;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

import javax.sound.sampled.*;
import java.io.File;

public class WavPlayer extends Thread {
    private Clip clip;
    private String path;
    private int volumePercent = 100; // Default volume is 100%
    private FloatControl volumeControl;
    
    public void play(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            // Get the volume control if available
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                setVolume(volumePercent); // Apply the current volume setting
            } else {
                System.out.println("Volume control not supported for this audio format");
            }
            
            clip.start();
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }
    
    /**
     * Sets the volume as a percentage (0 to 100)
     * @param percent Volume percentage (0 = mute, 100 = maximum volume)
     */
    public void setVolume(int percent) {
        // Ensure the percentage is within valid range
        volumePercent = Math.max(0, Math.min(100, percent));
        
        if (volumeControl != null) {
            // Convert percentage to decibel gain
            float minGain = volumeControl.getMinimum();
            float maxGain = volumeControl.getMaximum();
            
            // Calculate gain value based on percentage
            // Logarithmic scale is more natural for human hearing
            float gain;
            if (volumePercent == 0) {
                gain = minGain; // Mute
            } else {
                // Convert percentage to a logarithmic scale
                float range = maxGain - minGain;
                gain = minGain + (range * (float)(Math.log10(volumePercent) / 2));
            }
            
            // Apply the gain
            volumeControl.setValue(gain);
        }
    }
    
    /**
     * Alternative linear volume scaling (simpler but less natural)
     */
    public void setVolumeLinear(int percent) {
        volumePercent = Math.max(0, Math.min(100, percent));
        
        if (volumeControl != null) {
            float minGain = volumeControl.getMinimum();
            float maxGain = volumeControl.getMaximum();
            float range = maxGain - minGain;
            
            // Linear scaling
            float gain = minGain + (range * volumePercent / 100.0f);
            volumeControl.setValue(gain);
        }
    }
    
    /**
     * Gets the current volume percentage
     * @return Current volume as percentage (0-100)
     */
    public int getVolume() {
        return volumePercent;
    }
    
    public void stopAudio() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
            volumeControl = null;
        }
    }
    
    @Override
    public void run() {
        System.out.println("running wavplayer at volume: " + volumePercent + "%");
        play(path);
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public WavPlayer(String audioFilePath) {
        this.path = audioFilePath;
    }
    
    /**
     * Overloaded constructor with initial volume
     * @param audioFilePath Path to audio file
     * @param initialVolume Initial volume percentage (0-100)
     */
    public WavPlayer(String audioFilePath, int initialVolume) {
        this.path = audioFilePath;
        setVolume(initialVolume);
    }
    
    /**
     * Cleanup method
     */
    public void cleanup() {
        stopAudio();
        if (clip != null) {
            clip.close();
        }
    }
}