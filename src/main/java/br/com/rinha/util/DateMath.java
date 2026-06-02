package br.com.rinha.util;

public final class DateMath {
    private DateMath() {
    }

    public static int isoHour(String iso) {
        return iso.length() >= 13 ? (iso.charAt(11) - '0') * 10 + (iso.charAt(12) - '0') : 0;
    }

    public static double minutesDelta(String current, String previous) {
        return (daysFromIso(current) - daysFromIso(previous)) * 1440.0 + isoMinuteOfDay(current) - isoMinuteOfDay(previous);
    }

    public static int mondayBasedDow(String iso) {
        int dow = (daysFromIso(iso) + 3) % 7;
        return dow < 0 ? dow + 7 : dow;
    }

    private static int isoMinuteOfDay(String iso) {
        if (iso.length() < 16) return 0;
        int hour = (iso.charAt(11) - '0') * 10 + (iso.charAt(12) - '0');
        int min = (iso.charAt(14) - '0') * 10 + (iso.charAt(15) - '0');
        return hour * 60 + min;
    }

    private static int daysFromIso(String iso) {
        if (iso.length() < 10) return 0;
        int y = (iso.charAt(0) - '0') * 1000 + (iso.charAt(1) - '0') * 100 + (iso.charAt(2) - '0') * 10 + iso.charAt(3) - '0';
        int m = (iso.charAt(5) - '0') * 10 + iso.charAt(6) - '0';
        int d = (iso.charAt(8) - '0') * 10 + iso.charAt(9) - '0';
        y -= m <= 2 ? 1 : 0;
        int era = Math.floorDiv(y, 400);
        int yoe = y - era * 400;
        int mp = m + (m > 2 ? -3 : 9);
        int doy = (153 * mp + 2) / 5 + d - 1;
        int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }
}
