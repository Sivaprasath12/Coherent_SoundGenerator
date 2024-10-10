package io.github.coherent.sound_generator.generators;

import java.util.Random;

public class NoiseGenerator extends baseGenerator {

    private Random random = new Random();

    @Override
    public short getValue(double phase, double period) {
        // Generate random noise between -Short.MAX_VALUE and Short.MAX_VALUE
//        return (short) (random.nextInt(Short.MAX_VALUE * 2 + 1) - Short.MAX_VALUE);
        return (short) (random.nextGaussian() * Short.MAX_VALUE);
    }
}
