import java.io.File;
import javax.sound.sampled.*;

public class SoundManager {

    private static Clip backgroundClip;

    public static void playLoop(String filename) {
        stop();
        try (AudioInputStream audio = getPlayableAudioStream(filename)) {
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audio);
            setVolume(backgroundClip, -10.0f); // background softer
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playOnce(String filename) {
        try (AudioInputStream audio = getPlayableAudioStream(filename)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            setVolume(clip, 6.0f); // sound effects louder
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static AudioInputStream getPlayableAudioStream(String filename) throws Exception {
        AudioInputStream audio = AudioSystem.getAudioInputStream(new File(filename));
        AudioFormat baseFormat = audio.getFormat();
        AudioFormat playableFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.getSampleRate(),
            16,
            baseFormat.getChannels(),
            baseFormat.getChannels() * 2,
            baseFormat.getSampleRate(),
            false
        );
        if (AudioSystem.isConversionSupported(playableFormat, baseFormat)) {
            return AudioSystem.getAudioInputStream(playableFormat, audio);
        }
        return audio;
    }

    public static void stop() {
        if (backgroundClip != null) {
            backgroundClip.stop();
            backgroundClip.close();
        }
    }

    private static void setVolume(Clip clip, float gain) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = volume.getMinimum();
            float max = volume.getMaximum();
            float newGain = Math.max(min, Math.min(max, gain));
            volume.setValue(newGain);
        }
    }
}