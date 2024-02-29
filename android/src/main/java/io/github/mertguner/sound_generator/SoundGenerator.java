package io.github.mertguner.sound_generator;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import java.util.Random;

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

    //--
    int frequency;
    int sampleRate;
    int actualVolume;
    int numSamples;
    int s;

    int noise_frequency;
    int noise_sampleRate;
    int noise_actualVolume;
    int noise_numSamples;
    int noise_s;
    //--

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
        generator_noise.setDecibel(10);
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

            System.out.println("sxkjaxjcuyjc samplerate: "+sampleRate);
            System.out.println("sxkjaxjcuyjc buffersamplesize: "+minSamplesSize);

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        System.out.println("sxkjaxjcuyjc data: "+generator.getData());
                        audioTrack.write(generator.getData(), 0, minSamplesSize);
                    }
                }
            }
        }
        );

        isPlayingStreamHandler.change(true);

        bufferThread.start();
    }

    public void startPlayback2(int start_frequency, int start_sampleRate, int start_actualVolume, int start_numSamples, int start_s) {
        frequency = start_frequency;
        sampleRate = start_sampleRate;
        actualVolume = start_actualVolume;
        numSamples = start_numSamples;
        s = start_s;

//        playToneAndNoise(440.0, 5000);
        TestThread  testThread = new TestThread();
        testThread.start();


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

                    System.out.println("xjsgdcyugscyu 0: "+generator_noise.getNoiseData().toString());
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


    public float[] genTone(float increment, int volume, int numSamples){

        float angle = 0;
        float[] generatedSnd = new float[numSamples];
        for (int i = 0; i < numSamples; i++){
            generatedSnd[i] = (float) (Math.sin(angle)*volume/32768);
            angle += increment;
        }
        return generatedSnd;

    }




    public AudioTrack playSound(float[] generatedSnd, int ear, int sampleRate) {
        AudioTrack audioTrack = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    generatedSnd.length,
                    AudioTrack.MODE_STATIC);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioTrack.write(generatedSnd,0,generatedSnd.length,AudioTrack.WRITE_BLOCKING);
        }
        if (ear == 0) {
            audioTrack.setStereoVolume(0, AudioTrack.getMaxVolume());
        } else if (ear == 1) {
            audioTrack.setStereoVolume(AudioTrack.getMaxVolume(), 0 );
        } else {
            audioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
        }
        audioTrack.play();
        return audioTrack;

    }

    public int randomTime(){

        double num = Math.random();
        return (int) (1500+1500*num);
    }

    public class TestThread extends Thread {

        public void run() {
            AudioTrack audioTrack = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {



                float increment = (float) (2*Math.PI) * frequency / sampleRate;
                System.out.println("sxkjaxjcuyjc data: "+increment);
                audioTrack = playSound(genTone(increment,actualVolume, numSamples), s, sampleRate);

//                float increment = (float) (2*Math.PI) * 1000 / 44100;
//                audioTrack = playSound(genTone(increment,10922, 44100), 0, 44100);
            }

            try {
                Thread.sleep(randomTime());
            } catch (InterruptedException e) {}
            audioTrack.release();
        }
    }

    public void startPlayback3(int start_sampleRate, int start_actualVolume, int start_numSamples,  int s) {
        noise_sampleRate = start_sampleRate;
        noise_actualVolume = start_actualVolume;
        noise_numSamples = start_numSamples;
        noise_s = s;
        TestThread2 testThread = new TestThread2();
        testThread.start();
    }

    public float[] genNoise(int volume, int numSamples) {
        float[] generatedSnd = new float[numSamples];
        Random random = new Random();
        for (int i = 0; i < numSamples; ++i) {
            // Generate random noise within the range [-1, 1] and scale by the volume
            generatedSnd[i] = (random.nextFloat() * 2 - 1) * volume / 32768;
        }
        return generatedSnd;
    }

    public class TestThread2 extends Thread {
        public void run() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioTrack = playSound(genNoise(noise_actualVolume, noise_numSamples), noise_s, noise_sampleRate);
            }
           /* try {
                Thread.sleep(randomTime());
            } catch (InterruptedException e) {}
            if (audioTrack != null) {
                audioTrack.release();
            }*/
        }
    }


    public void stopPlayback3() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
    }


    public void playToneAndNoise(double toneFreq, int durationMs) {
        final int sampleRate = 44100;
        final int numSamples = durationMs * sampleRate / 1000;
        final double toneSamples[] = new double[numSamples];
        final double noiseSamples[] = new double[numSamples];
        final short[] finalSamples = new short[numSamples * 2]; // times 2 for stereo
        final double tonePhaseIncrement = (2 * Math.PI) * toneFreq / sampleRate;
        double tonePhase = 0.0;
        Random random = new Random();

        for (int i = 0; i < numSamples; ++i) {
            toneSamples[i] = Math.sin(tonePhase);
            tonePhase += tonePhaseIncrement;

            // Generate white noise by random values
            noiseSamples[i] = (random.nextDouble() * 2.0) - 1.0; // Range [-1.0, 1.0]

            // Convert to 16 bit pcm sound array
            final short valTone = (short) ((toneSamples[i] * 32767)); // scale to max amplitude
            final short valNoise = (short) ((noiseSamples[i] * 32767)); // scale to max amplitude

            finalSamples[2 * i] = 0; // Left channel - pure tone
            finalSamples[2 * i + 1] = valNoise; // Right channel - noise
        }

        // Instantiate and play the audio track
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                finalSamples.length * 2, AudioTrack.MODE_STATIC);

        audioTrack.write(finalSamples, 0, finalSamples.length);
        audioTrack.play();
    }


}