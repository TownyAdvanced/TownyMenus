package io.github.townyadvanced.townymenus.utils;

public class Mth {
    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}
