package br.com.rinha.vector;

import br.com.rinha.model.Models.*;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.Map;

public class Vectorizer {
    public static class Config {
        public double max_amount;
        public double max_installments;
        public double amount_vs_avg_ratio;
        public double max_minutes;
        public double max_km;
        public double max_tx_count_24h;
        public double max_merchant_avg_amount;
    }

    public static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    public static double[] vectorize(TransactionRequest req, Config cfg, Map<String, Double> mccRisk) {
        double[] v = new double[14];

        OffsetDateTime requestedAt = safeDate(req.transaction.requested_at);

        // 0: amount
        v[0] = clamp(req.transaction.amount / cfg.max_amount);

        // 1: installments
        v[1] = clamp((double) req.transaction.installments / cfg.max_installments);

        // 2: amount_vs_avg
        if (req.customer.avg_amount > 0) {
            v[2] = clamp((req.transaction.amount / req.customer.avg_amount) / cfg.amount_vs_avg_ratio);
        } else {
            v[2] = 1.0;
        }

        // 3: hour_of_day
        if (requestedAt != null) {
            v[3] = (double) requestedAt.getHour() / 23.0;
        } else {
            v[3] = 0.0;
        }

        // 4: day_of_week
        if (requestedAt != null) {
            int dow = requestedAt.getDayOfWeek().getValue() - 1;
            v[4] = (double) dow / 6.0;
        } else {
            v[4] = 0.0;
        }

        // 5 & 6: last transaction
        if (req.last_transaction == null) {
            v[5] = -1.0;
            v[6] = -1.0;
        } else {
            OffsetDateTime lastAt = safeDate(req.last_transaction.timestamp);

            if (lastAt != null && requestedAt != null) {
                double minutes = Duration.between(lastAt, requestedAt).toMinutes();
                v[5] = clamp(minutes / cfg.max_minutes);
            } else {
                v[5] = -1.0;
            }

            v[6] = clamp(req.last_transaction.km_from_current / cfg.max_km);
        }

        // 7: km_from_home
        v[7] = clamp(req.terminal.km_from_home / cfg.max_km);

        // 8: tx_count_24h
        v[8] = clamp((double) req.customer.tx_count_24h / cfg.max_tx_count_24h);

        // 9: is_online
        v[9] = req.terminal.is_online ? 1.0 : 0.0;

        // 10: card_present
        v[10] = req.terminal.card_present ? 1.0 : 0.0;

        // 11: unknown_merchant
        boolean known = false;
        if (req.customer.known_merchants != null) {
            for (String m : req.customer.known_merchants) {
                if (m.equals(req.merchant.id)) {
                    known = true;
                    break;
                }
            }
        }
        v[11] = known ? 0.0 : 1.0;

        // 12: mcc_risk
        v[12] = mccRisk.getOrDefault(req.merchant.mcc, 0.5);

        // 13: merchant_avg_amount
        v[13] = clamp(req.merchant.avg_amount / cfg.max_merchant_avg_amount);

        return v;
    }

    private static OffsetDateTime safeDate(String value) {
        if (value == null) return OffsetDateTime.MIN;

        value = value.trim();
        if (value.isEmpty()) return OffsetDateTime.MIN;

        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            return OffsetDateTime.MIN;
        }
    }

    public static double euclideanDistanceSq(double[] v1, double[] v2) {
        double distSq = 0;
        for (int i = 0; i < v1.length; i++) {
            double diff = v1[i] - v2[i];
            distSq += diff * diff;
        }
        return distSq;
    }
}
