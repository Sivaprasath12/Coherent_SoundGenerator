package io.github.coherent.sound_generator.generators;

public class sinusoidalGenerator extends baseGenerator {
    public short getValue(double phase, double period) {
        return (short) (Short.MAX_VALUE * Math.sin(phase));
    }
}
