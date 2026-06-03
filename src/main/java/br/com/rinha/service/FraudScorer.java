package br.com.rinha.service;

import br.com.rinha.util.DateMath;
import br.com.rinha.util.FastJson;

public final class FraudScorer {

    private static final double[] W = {
            -8.523164709, 1.863921269, 3.094092674, 0.5529062283, -0.2422058805,
            -0.06415558462, -2.511498622, 0.7589419852, 1.524335692, 3.462601206,
            -0.01745284273, -0.208227002, 0.5689366744, 0.9938504886, -22.91874404,
            -0.1604114719, 2.358111607, 4.211645929, 3.049510263
    };

    public double score(String body) {
        String requestedAt = FastJson.string(body, "\"requested_at\"", "");
        String lastTs = FastJson.string(body, "\"timestamp\"", "");
        int merchantPos = body.indexOf("\"merchant\"");
        int merchantStart = merchantPos >= 0 ? merchantPos : body.length();
        String merchantId = FastJson.string(body, "\"id\"", "", merchantStart);
        String mcc = FastJson.string(body, "\"mcc\"", "", merchantStart);

        double amount = FastJson.number(body, "\"amount\"", 0);
        double installments = FastJson.number(body, "\"installments\"", 0);
        double customerAvg = FastJson.number(body, "\"avg_amount\"", 0.01);
        double txCount = FastJson.number(body, "\"tx_count_24h\"", 0);
        double kmHome = FastJson.number(body, "\"km_from_home\"", 0);
        double kmLast = FastJson.number(body, "\"km_from_current\"", 0);
        double merchantAvg = FastJson.number(body, "\"avg_amount\"", 0, merchantStart);
        boolean hasLast = !body.contains("\"last_transaction\":null") && !lastTs.isEmpty();

        double safeCustomerAvg = customerAvg > 0 ? customerAvg : 0.01;
        double z = W[0];
        z += W[1] * clamp(amount / 10000.0);
        z += W[2] * clamp(installments / 12.0);
        z += W[3] * clamp((amount / safeCustomerAvg) / 10.0);
        z += W[4] * (DateMath.isoHour(requestedAt) / 23.0);
        z += W[5] * (DateMath.mondayBasedDow(requestedAt) / 6.0);
        z += W[6] * (hasLast ? clamp(DateMath.minutesDelta(requestedAt, lastTs) / 1440.0) : -1.0);
        z += W[7] * (hasLast ? clamp(kmLast / 1000.0) : -1.0);
        z += W[8] * clamp(kmHome / 1000.0);
        z += W[9] * clamp(txCount / 20.0);
        z += W[10] * (FastJson.bool(body, "\"is_online\"") ? 1.0 : 0.0);
        z += W[11] * (FastJson.bool(body, "\"card_present\"") ? 1.0 : 0.0);
        z += W[12] * (merchantUnknown(body, merchantId) ? 1.0 : 0.0);
        z += W[13] * mccRisk(mcc);
        z += W[14] * clamp(merchantAvg / 10000.0);
        z += W[15] * (hasLast ? 1.0 : 0.0);
        z += W[16] * (Math.log1p(amount / safeCustomerAvg) / Math.log(101.0));
        z += W[17] * (Math.log1p(kmHome) / Math.log(1001.0));
        z += W[18] * (Math.log1p(kmLast) / Math.log(1001.0));

        if (z > 30) return 1.0;
        if (z < -30) return 0.0;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private static double clamp(double x) {
        return x < 0 ? 0 : Math.min(x, 1);
    }

    private static boolean merchantUnknown(String body, String merchantId) {
        int knownMerchants = body.indexOf("\"known_merchants\"");
        int merchant = body.indexOf("\"merchant\"");
        if (knownMerchants < 0 || merchant < 0 || knownMerchants > merchant || merchantId.isEmpty()) return true;
        int pos = body.indexOf(merchantId, knownMerchants);
        return pos < 0 || pos >= merchant;
    }

    private static double mccRisk(String mcc) {
        return switch (mcc) {
            case "5411" -> 0.15;
            case "5812" -> 0.30;
            case "5912" -> 0.20;
            case "5944" -> 0.45;
            case "7801" -> 0.80;
            case "7802" -> 0.75;
            case "7995" -> 0.85;
            case "4511" -> 0.35;
            case "5311" -> 0.25;
            case "5999" -> 0.50;
            default -> 0.50;
        };
    }
}
