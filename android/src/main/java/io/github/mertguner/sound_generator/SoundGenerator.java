package io.github.mertguner.sound_generator;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import io.github.mertguner.sound_generator.generators.NoiseGenerator;
import io.github.mertguner.sound_generator.generators.sawtoothGenerator;
import io.github.mertguner.sound_generator.generators.signalDataGenerator;
import io.github.mertguner.sound_generator.generators.sinusoidalGenerator;
import io.github.mertguner.sound_generator.generators.squareWaveGenerator;
import io.github.mertguner.sound_generator.generators.triangleGenerator;
import io.github.mertguner.sound_generator.handlers.isPlayingStreamHandler;
import io.github.mertguner.sound_generator.handlers.isPlayingStreamNoiseHandler;
import io.github.mertguner.sound_generator.models.WaveTypes;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class SoundGenerator {

    private Thread buffernoiseThread;
    private Thread bufferThread;
    private AudioTrack audioTrack;

    private AudioTrack audioTracknoise;
    private signalDataGenerator generator, generator_noise;
    private boolean isPlaying = false;
    private boolean isPlayingnoise = false;
    private int minSamplesSize;
    private WaveTypes waveType = WaveTypes.SINUSOIDAL;
    private float rightVolume = 1, leftVolume = 1;

    public void setAutoUpdateOneCycleSample(boolean autoUpdateOneCycleSample) {
        if (generator != null)
            generator.setAutoUpdateOneCycleSample(autoUpdateOneCycleSample);
        generator_noise.setAutoUpdateOneCycleSample(autoUpdateOneCycleSample);

    }

    public int getSampleRate() {
        if (generator != null)
            return generator.getSampleRate();
        return 0;
    }

    public void setSampleRate(int sampleRate) {
        if (generator != null)
            generator.setSampleRate(sampleRate);
    }

    public void refreshOneCycleData() {
        if (generator != null)
            generator.createOneCycleData(true);
        generator_noise.createOneCycleData_noise(true);
    }

    public void setFrequency(float v) {
        if (generator != null)
            generator.setFrequency(v);
        generator_noise.setFrequency(125);
    }

    public float getFrequency() {
        if (generator != null)
            return generator.getFrequency();
        return 0;
    }

    public void setDecibel(float v) {
        if (generator != null)
            generator.setDecibel(v);
        generator_noise.setDecibel(v);
    }

    public void setNoiseDecibel(float v) {
        if (generator_noise != null)
            generator_noise.setNoiseDecibel(v);
    }

    public float getDecibel() {
        if (generator != null)
            return generator.getDecibel();
        return 0;
    }

    public void setBalance(float balance) {
        balance = Math.max(-1, Math.min(1, balance));

        rightVolume = (balance >= 0) ? 1 : (balance == -1) ? 0 : (1 + balance);
        leftVolume = (balance <= 0) ? 1 : (balance == 1) ? 0 : (1 - balance);
        if (audioTrack != null) {
            audioTrack.setStereoVolume(leftVolume, rightVolume);
        }
    }


    public void setVolume(float volume) {
        volume = Math.max(0, Math.min(1, volume));

        if (audioTrack != null) {
            audioTrack.setStereoVolume(leftVolume * volume, rightVolume * volume);
        }
    }

    public void setWaveform(WaveTypes waveType) {
        if (this.waveType.equals(waveType) || (generator == null))
            return;

        this.waveType = waveType;

        if (waveType.equals(WaveTypes.SINUSOIDAL))
            generator.setGenerator(new sinusoidalGenerator());
        else if (waveType.equals(WaveTypes.TRIANGLE))
            generator.setGenerator(new triangleGenerator());
        else if (waveType.equals(WaveTypes.SQUAREWAVE))
            generator.setGenerator(new squareWaveGenerator());
        else if (waveType.equals(WaveTypes.SAWTOOTH))
            generator.setGenerator(new sawtoothGenerator());
        else if (waveType.equals(WaveTypes.NOISE))
            generator.setGenerator(new NoiseGenerator());
    }

    public boolean init(int sampleRate) {
        try {
            minSamplesSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            generator = new signalDataGenerator(minSamplesSize, sampleRate);

            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minSamplesSize,
                    AudioTrack.MODE_STREAM);


            //noise

//            minSamplesSize = AudioTrack.getMinBufferSize(
//                    sampleRate,
//                    AudioFormat.CHANNEL_OUT_MONO,
//                    AudioFormat.ENCODING_PCM_16BIT);

            generator_noise = new signalDataGenerator(minSamplesSize, sampleRate);

            audioTracknoise = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minSamplesSize,
                    AudioTrack.MODE_STREAM);

            return true;
        }catch (Exception ex)
        {
            return false;
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void startPlayback() {
        if (bufferThread != null || audioTrack == null) return;
        isPlaying = true;

        bufferThread = new Thread(new Runnable() {
            @Override
            public void run() {
                audioTrack.flush();
                audioTrack.setPlaybackHeadPosition(0);
                audioTrack.play();
                while (isPlaying) {
                    audioTrack.write(generator.getData(), 0, minSamplesSize);
                }
            }
        }
        );

        isPlayingStreamHandler.change(true);

        bufferThread.start();
    }

    public void stopPlayback() {
        if (bufferThread == null) return;

        isPlaying = false;

        try {
            bufferThread.join(); //Waiting thread
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        isPlayingStreamHandler.change(false);
        bufferThread = null;

        if (audioTrack != null) {
            audioTrack.stop();
        }
    }

    public void release() {
        if (isPlaying())
            stopPlayback();
        audioTrack.release();
    }



    public void startnoisePlayback() {
        if (buffernoiseThread != null || audioTracknoise == null) return;

        isPlayingnoise = true;

        buffernoiseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                audioTracknoise.flush();
                audioTracknoise.setPlaybackHeadPosition(0);
                audioTracknoise.play();
                while (isPlayingnoise) {
                    audioTracknoise.write(generator_noise.getNoiseData(), 0, minSamplesSize);
                }
            }
        }
        );

        isPlayingStreamNoiseHandler.change(true);

        buffernoiseThread.start();
    }

    public void stopnoisePlayback() {
        if (buffernoiseThread == null) return;

        isPlayingnoise = false;

        try {
            buffernoiseThread.join(); //Waiting thread
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        isPlayingStreamNoiseHandler.change(false);
        buffernoiseThread = null;

        if (audioTracknoise != null) {
            audioTracknoise.stop();
        }
    }


}
