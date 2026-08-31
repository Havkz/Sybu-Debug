package com.havkz.sybudebug.activity;

import meteordevelopment.meteorclient.utils.render.color.Color;

public final class ActivityHeatmap {
    private ActivityHeatmap() {}

    public static double horizontalDistanceSquared(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return dx * dx + dz * dz;
    }

    public static double normalize(double distanceSquared, double nearDistance, double farDistance) {
        double range = Math.max(1, farDistance - nearDistance);
        return Math.max(0, Math.min(1, (Math.sqrt(distanceSquared) - nearDistance) / range));
    }

    public static Color color(double normalized, int alpha) {
        double t = Math.max(0, Math.min(1, normalized));
        int red;
        int green;
        if (t < 0.5) {
            red = 255;
            green = (int) Math.round(t * 2 * 220);
        } else {
            red = (int) Math.round(255 - (t - 0.5) * 2 * 215);
            green = 220;
        }
        return new Color(red, green, 40, alpha);
    }

    public static Color color(double normalized, Color near, Color far) {
        return color(normalized, near, far, 100);
    }

    public static Color color(double normalized, Color near, Color far, int opacityPercent) {
        double t = Math.max(0, Math.min(1, normalized));
        return new Color(
            mix(near.r, far.r, t),
            mix(near.g, far.g, t),
            mix(near.b, far.b, t),
            mix(near.a, far.a, t) * Math.max(0, Math.min(100, opacityPercent)) / 100);
    }

    private static int mix(int from, int to, double amount) { return (int) Math.round(from + (to - from) * amount); }
}
