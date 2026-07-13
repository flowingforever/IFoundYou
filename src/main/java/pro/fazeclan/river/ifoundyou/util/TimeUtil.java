package pro.fazeclan.river.ifoundyou.util;

public class TimeUtil {

    public static String ticksIntoReadableFormat(int ticks) {
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

}
