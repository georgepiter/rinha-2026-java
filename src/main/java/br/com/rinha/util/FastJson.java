package br.com.rinha.util;

public final class FastJson {

    private FastJson() {
    }

    public static double number(String source, String key, double fallback) {
        int pos = source.indexOf(key);
        if (pos < 0) return fallback;
        pos += key.length();

        while (pos < source.length() && isSeparator(source.charAt(pos))) pos++;

        int end = pos;
        while (end < source.length()) {
            char c = source.charAt(end);
            if (!(c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || (c >= '0' && c <= '9'))) break;
            end++;
        }

        try {
            return Double.parseDouble(source.substring(pos, end));
        } catch (Exception ignored) {
            return fallback;
        }
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
