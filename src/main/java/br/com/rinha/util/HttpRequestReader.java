package br.com.rinha.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class HttpRequestReader {

    private HttpRequestReader() {
    }

    public static int read(InputStream in, byte[] buffer) throws Exception {
        int total = 0;
        int contentLength = -1;
        int bodyStart = -1;

        while (total < buffer.length) {
            int read = in.read(buffer, total, buffer.length - total);
            if (read <= 0) break;
            total += read;

            if (bodyStart < 0) {
                bodyStart = bodyStart(buffer, total);
                if (bodyStart >= 0) contentLength = contentLength(buffer, bodyStart);
            }
            if (bodyStart >= 0 && contentLength >= 0 && total >= bodyStart + contentLength) break;
        }

        return total;
    }

    public static boolean startsWith(byte[] bytes, int size, String prefix) {
        if (size < prefix.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (bytes[i] != (byte) prefix.charAt(i)) return false;
        }
        return true;
    }

    public static int bodyStart(byte[] bytes, int size) {
        for (int i = 3; i < size; i++) {
            if (bytes[i - 3] == '\r' && bytes[i - 2] == '\n' && bytes[i - 1] == '\r' && bytes[i] == '\n') {
                return i + 1;
            }
        }
        return -1;
    }

    private static int contentLength(byte[] bytes, int bodyStart) {
        String headers = new String(bytes, 0, bodyStart, StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
        int pos = headers.indexOf("content-length:");
        if (pos < 0) return 0;
        pos += "content-length:".length();
        while (pos < headers.length() && headers.charAt(pos) == ' ') pos++;
        int end = pos;
        while (end < headers.length() && Character.isDigit(headers.charAt(end))) end++;
        return Integer.parseInt(headers.substring(pos, end));
    }
}
