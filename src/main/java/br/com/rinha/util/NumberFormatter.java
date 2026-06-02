package br.com.rinha.util;

public final class NumberFormatter {

    private NumberFormatter() {
    }

    public static String format4(double value) {
        long scaled = (long) (value * 10000.0 + 0.5);
        long whole = scaled / 10000;
        long fraction = scaled % 10000;
        if (fraction < 10) return whole + ".000" + fraction;
        if (fraction < 100) return whole + ".00" + fraction;
        if (fraction < 1000) return whole + ".0" + fraction;
        return whole + "." + fraction;
    }
}
