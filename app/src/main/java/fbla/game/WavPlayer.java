package fbla.game;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class WavPlayer extends Thread{
    private Clip clip;
    private String path;
    public void play(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }

    public void stopAudio() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }
    
    @Override
    public void run(){
        System.out.println("running wavplayer");
        play(path);
    }

    public void setPath(String path){
        this.path = path;
    }

    public WavPlayer(String audioFilePath){
        this.path = audioFilePath;
    }
}
