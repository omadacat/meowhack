package cat.omada.meowhack.util.math;

import java.util.List;
import java.util.Random;

public class MathUtil {

    private static final Random r = new Random();

    public static int clamp(int num, int min, int max) {
        return num < min ? min : Math.min(num, max);
    }

    public static float rad(float angle) {
        return (float) (angle * Math.PI / 180);
    }

    public static int pickRandom(List<?> list) {
        if (list.size() == 1) return 0;

        int num = r.nextInt(0, list.size()-1);
        return num;
    }

    public static double random(double max, double min) {
        return Math.random() * (max - min) + min;
    }

    public static float squared(float value) {
        return value*value;
    }

    public static int squared(int value) {
        return value*value;
    }

}
