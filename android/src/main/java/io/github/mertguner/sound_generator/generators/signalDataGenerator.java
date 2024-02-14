package io.github.mertguner.sound_generator.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.mertguner.sound_generator.handlers.getOneCycleDataHandler;

public class signalDataGenerator {

    private Random random = new Random();
    private final float _2Pi = 2.0f * (float) Math.PI;

    private int sampleRate = 48000;
    private float phCoefficient = _2Pi / (float) sampleRate;
    private float smoothStep = 1f / (float) sampleRate * 20f;

    private float frequency = 50;

    private float decibel = 20;
    private float noise_decibel = 30;
    private baseGenerator generator = new sinusoidalGenerator();

    private baseGenerator generator_noise = new NoiseGenerator();

    private short[] backgroundBuffer;
    private short[] buffer;
    private List<Integer> oneCycleBuffer = new ArrayList<>();
    private int bufferSamplesSize;
    private float ph = 0;
    private float oldFrequency = 50;
    private boolean creatingNewData = false;
    private boolean autoUpdateOneCycleSample = false;

    public boolean isAutoUpdateOneCycleSample() { return autoUpdateOneCycleSample; }
    public void setAutoUpdateOneCycleSample(boolean autoUpdateOneCycleSample) { this.autoUpdateOneCycleSample = autoUpdateOneCycleSample; }

    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        phCoefficient = _2Pi / (float) sampleRate;
        smoothStep = 1f / (float) sampleRate * 20f;
    }

    public baseGenerator getGenerator() {
        return generator;
    }
    public void setGenerator(baseGenerator generator) {
        this.generator = generator;

        createOneCycleData();
    }

    public float getFrequency() {
        return frequency;
    }
    public void setFrequency(float frequency) {
        this.frequency = frequency;
        createOneCycleData();
    }

    public float getDecibel() {
        return decibel;
    }

    public void setDecibel(float decibel) {
        this.decibel = decibel;
    }
    public void setNoiseDecibel(float decibel) {
        this.noise_decibel = decibel;
    }

    public signalDataGenerator(int bufferSamplesSize, int sampleRate) {
        this.bufferSamplesSize = bufferSamplesSize;
        backgroundBuffer = new short[bufferSamplesSize];
        buffer = new short[bufferSamplesSize];
        setSampleRate(sampleRate);
        updateNoiseData();
        updateData();

        createOneCycleData();
    }
    /*private void updateData() {
        creatingNewData = true;
        for (int i = 0; i < bufferSamplesSize; i++) {
            oldFrequency += ((frequency - oldFrequency) * smoothStep);
            float value = generator.getValue(ph, _2Pi);

            // Do not apply decibel level here, apply it after generating the value
            // float amplitude = (float) Math.pow(10, decibel / 20.0);
            // value *= amplitude;

            backgroundBuffer[i] = (short) value;
            ph += (oldFrequency * phCoefficient);

            // Performance optimization: use if block instead of modulus operation
            if (ph > _2Pi) {
                ph -= _2Pi;
            }
        }

        // Apply the specified decibel level to the generated samples after generating the values
        float amplitude = (float) Math.pow(10, decibel / 20.0);
        for (int i = 0; i < bufferSamplesSize; i++) {
            backgroundBuffer[i] *= amplitude;
        }

        creatingNewData = false;
    }*/

    /*
    //without beep sound
    private void updateData() {
        creatingNewData = true;

        // Generate the waveform without applying decibel level to each sample
        for (int i = 0; i < bufferSamplesSize; i++) {
            oldFrequency += ((frequency - oldFrequency) * smoothStep);
            float value = generator.getValue(ph, _2Pi);
            backgroundBuffer[i] = (short) value;
            ph += (oldFrequency * phCoefficient);

            // Performance optimization: use if block instead of modulus operation
            if (ph > _2Pi) {
                ph -= _2Pi;
            }
        }

        // Apply the specified decibel level to the entire waveform
        float maxAmplitude = findMaxAmplitude(backgroundBuffer);
        float amplitude = (float) Math.pow(10, decibel / 20.0);
        for (int i = 0; i < bufferSamplesSize; i++) {
            backgroundBuffer[i] = (short) (backgroundBuffer[i] * amplitude / maxAmplitude);
        }

        creatingNewData = false;
    }
*/
    //with beepsound
    private void updateData() {
        creatingNewData = true;

        int beepDurationSamples = (int) (sampleRate * 0.5); // Adjust the duration of the beep sound

        // Generate the waveform for the beep sound
        for (int i = 0; i < bufferSamplesSize; i++) {
            if (i < beepDurationSamples) {
                oldFrequency += ((frequency - oldFrequency) * smoothStep);
                float value = generator.getValue(ph, _2Pi);

                // Apply envelope to create rising and falling edges
                float envelope = 0.5f - 0.5f * (float)Math.cos(2.0 * Math.PI * i / beepDurationSamples);
                value *= envelope;

                backgroundBuffer[i] = (short) value;
                ph += (oldFrequency * phCoefficient);

                // Performance optimization: use if block instead of modulus operation
                if (ph > _2Pi) {
                    ph -= _2Pi;
                }
            } else {
                backgroundBuffer[i] = 0; // Set the remaining samples to zero for silence
            }
        }

        // Apply the specified decibel level to the entire waveform
        float maxAmplitude = findMaxAmplitude(backgroundBuffer);
        float amplitude = (float) Math.pow(10, decibel / 20.0);
        for (int i = 0; i < bufferSamplesSize; i++) {
            backgroundBuffer[i] = (short) (backgroundBuffer[i] * amplitude / maxAmplitude);
        }

        creatingNewData = false;
    }




    // Helper method to find the maximum amplitude in the waveform
    private float findMaxAmplitude(short[] waveform) {
        float maxAmplitude = 0;
        for (short sample : waveform) {
            float sampleValue = Math.abs(sample);
            if (sampleValue > maxAmplitude) {
                maxAmplitude = sampleValue;
            }
        }
        return maxAmplitude;
    }


    private void updateNoiseData() {
        creatingNewData = true;
        for (int i = 0; i < bufferSamplesSize; i++) {
            oldFrequency += ((frequency - oldFrequency) * smoothStep);
            float value = generator_noise.getValue(ph, _2Pi);

            // Apply the specified decibel level to the generated sample
            float amplitude = (float) Math.pow(10, noise_decibel / 40.0);
            value *= amplitude;

            backgroundBuffer[i] = (short) value;
            ph += (oldFrequency * phCoefficient);

            // Performance optimization: use if block instead of modulus operation
            if (ph > _2Pi) {
                ph -= _2Pi;
            }
        }
        creatingNewData = false;
    }

    public short[] getData() {
        if (!creatingNewData) {
            System.arraycopy(backgroundBuffer, 0, buffer, 0, bufferSamplesSize);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    updateData();
                }
            }).start();
        }
        return this.buffer;
    }

    public short[] getNoiseData() {
        if (!creatingNewData) {
            System.arraycopy(backgroundBuffer, 0, buffer, 0, bufferSamplesSize);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    updateNoiseData();
                }
            }).start();
        }
        return this.buffer;
    }


    public void createOneCycleData() {
        createOneCycleData(false);
    }

    public void createOneCycleData(boolean force) {
        if (generator == null || (!autoUpdateOneCycleSample && !force))
            return;

        int size = Math.round(_2Pi / (frequency * phCoefficient));

        oneCycleBuffer.clear();
        for (int i = 0; i < size; i++) {
            oneCycleBuffer.add((int)generator.getValue((frequency * phCoefficient) * (float) i, _2Pi));
        }
        oneCycleBuffer.add((int)generator.getValue(0, _2Pi));
        getOneCycleDataHandler.setData(oneCycleBuffer);
    }

    public void createOneCycleData_noise(boolean force) {
        if (generator_noise == null || (!autoUpdateOneCycleSample && !force))
            return;

        int size = Math.round(_2Pi / (frequency * phCoefficient));

        oneCycleBuffer.clear();
        for (int i = 0; i < size; i++) {
            oneCycleBuffer.add((int)generator_noise.getValue((frequency * phCoefficient) * (float) i, _2Pi));
        }
        oneCycleBuffer.add((int)generator_noise.getValue(0, _2Pi));
        getOneCycleDataHandler.setData(oneCycleBuffer);
    }

}
