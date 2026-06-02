package br.com.rinha.util;

public final class FastJson {

    private FastJson() {
    }

    public static double number(String source, String key, double fallback) {
        int pos = source.indexOf(key);
        if (pos < 0) return fallback;
        pos += key.length();

        while (pos < source.length() && isSeparator(source.charAt(pos))) pos++;
        if (pos >= source.length()) return fallback;

        double sign = 1.0;
        if (source.charAt(pos) == '-') {
            sign = -1.0;
            pos++;
        }

        double value = 0.0;
        boolean found = false;
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c < '0' || c > '9') break;
            value = value * 10.0 + c - '0';
            pos++;
            found = true;
        }

        if (pos < source.length() && source.charAt(pos) == '.') {
            pos++;
            double factor = 0.1;
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (c < '0' || c > '9') break;
                value += (c - '0') * factor;
                factor *= 0.1;
                pos++;
                found = true;
            }
        }

        return found ? value * sign : fallback;
    }

    public static boolean bool(String source, String key) {
        int pos = source.indexOf(key);
        if (pos < 0) return false;
        pos += key.length();

        while (pos < source.length() && isSeparator(source.charAt(pos))) pos++;
        return pos + 4 <= source.length() && source.startsWith("true", pos);
    }

    public static String string(String source, String key, String fallback) {
        int pos = source.indexOf(key);
        if (pos < 0) return fallback;
        pos += key.length();

        while (pos < source.length() && source.charAt(pos) != '"') pos++;
        if (pos >= source.length()) return fallback;

        int start = ++pos;
        while (pos < source.length() && source.charAt(pos) != '"') pos++;
        return pos > start ? source.substring(start, pos) : fallback;
    }

    private static boolean isSeparator(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == ':';
    }
}
