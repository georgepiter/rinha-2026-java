package br.com.rinha.controller;

import br.com.rinha.service.FraudScorer;
import br.com.rinha.util.HttpRequestReader;
import br.com.rinha.util.NumberFormatter;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class FraudController {

    private static final byte[] READY = "HTTP/1.1 204 No Content\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final double DECISION_THRESHOLD = 0.06;

    private final FraudScorer scorer = new FraudScorer();

    public void handle(byte[] request, int size, OutputStream out) throws Exception {
        if (HttpRequestReader.startsWith(request, size, "GET /ready")) {
            out.write(READY);
            out.flush();
            return;
        }

        if (!HttpRequestReader.startsWith(request, size, "POST /fraud-score")) {
            out.write(NOT_FOUND);
            out.flush();
            return;
        }

        int bodyStart = HttpRequestReader.bodyStart(request, size);
        if (bodyStart < 0) {
            sendOk(out, "{\"approved\":true,\"fraud_score\":0}");
            return;
        }

        String body = new String(request, bodyStart, size - bodyStart, StandardCharsets.UTF_8);
        double score = scorer.score(body);
        boolean approved = score < DECISION_THRESHOLD;
        sendOk(out, "{\"approved\":" + approved + ",\"fraud_score\":" + NumberFormatter.format4(score) + "}");
    }

    public static void sendOk(OutputStream out, String body) throws Exception {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + data.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(data);
        out.flush();
    }
}
