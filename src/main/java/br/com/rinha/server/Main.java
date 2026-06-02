package br.com.rinha.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main {

  private static final byte[] READY = "HTTP/1.1 204 No Content\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
  private static final double DECISION_THRESHOLD = 0.127;

  private static final double[] W = {
      -8.523164709, 1.863921269, 3.094092674, 0.5529062283, -0.2422058805,
      -0.06415558462, -2.511498622, 0.7589419852, 1.524335692, 3.462601206,
      -0.01745284273, -0.208227002, 0.5689366744, 0.9938504886, -22.91874404,
      -0.1604114719, 2.358111607, 4.211645929, 3.049510263
  };

  public static void main(String[] args) throws Exception {
    ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    try (ServerSocket server = new ServerSocket(8080, 4096)) {
      server.setReuseAddress(true);

      while (true) {
        Socket socket = server.accept();
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSoTimeout(15000);
        pool.execute(() -> handle(socket));
      }
    }
  }

  private static void handle(Socket socket) {
    try (socket) {
      InputStream in = socket.getInputStream();
      OutputStream out = socket.getOutputStream();
      byte[] buf = new byte[32768];

      while (true) {
        int n = readRequest(in, buf);
        if (n <= 0) return;

        if (startsWith(buf, n, "GET /ready")) {
          out.write(READY);
          out.flush();
          continue;
        }
        if (!startsWith(buf, n, "POST /fraud-score")) {
          out.write(NOT_FOUND);
          out.flush();
          continue;
        }

        int bodyStart = bodyStart(buf, n);
        if (bodyStart < 0) {
          send(out, "{\"approved\":true,\"fraud_score\":0}");
          continue;
        }
        String body = new String(buf, bodyStart, n - bodyStart, StandardCharsets.UTF_8);
        double score = calibrate(score(body));
        send(out, "{\"approved\":" + (score < DECISION_THRESHOLD ? "true" : "false")
            + ",\"fraud_score\":" + format4(score) + "}");
      }
    } catch (Exception ignored) {
      try {
        send(socket.getOutputStream(), "{\"approved\":true,\"fraud_score\":0}");
      } catch (Exception ignoredAgain) {
      }
    }
  }

  private static int readRequest(InputStream in, byte[] buf) throws Exception {
    int n = 0;
    int contentLength = -1;
    int bodyStart = -1;
    while (n < buf.length) {
      int r = in.read(buf, n, buf.length - n);
      if (r <= 0) break;
      n += r;
      if (bodyStart < 0) {
        bodyStart = bodyStart(buf, n);
        if (bodyStart >= 0) contentLength = contentLength(buf, bodyStart);
      }
      if (bodyStart >= 0 && contentLength >= 0 && n >= bodyStart + contentLength) break;
    }
    return n;
  }

  private static boolean startsWith(byte[] b, int n, String prefix) {
    if (n < prefix.length()) return false;
    for (int i = 0; i < prefix.length(); i++) {
      if (b[i] != (byte) prefix.charAt(i)) return false;
    }
    return true;
  }

  private static int bodyStart(byte[] b, int n) {
    for (int i = 3; i < n; i++) {
      if (b[i - 3] == '\r' && b[i - 2] == '\n' && b[i - 1] == '\r' && b[i] == '\n') return i + 1;
    }
    return -1;
  }

  private static int contentLength(byte[] b, int bodyStart) {
    String h = new String(b, 0, bodyStart, StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
    int p = h.indexOf("content-length:");
    if (p < 0) return 0;
    p += "content-length:".length();
    while (p < h.length() && h.charAt(p) == ' ') p++;
    int e = p;
    while (e < h.length() && Character.isDigit(h.charAt(e))) e++;
    return Integer.parseInt(h.substring(p, e));
  }

  private static void send(OutputStream out, String body) throws Exception {
    byte[] data = body.getBytes(StandardCharsets.UTF_8);
    out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
        + data.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
    out.write(data);
    out.flush();
  }

  private static String format4(double v) {
    long scaled = (long) (v * 10000.0 + 0.5);
    long whole = scaled / 10000;
    long frac = scaled % 10000;
    if (frac < 10) return whole + ".000" + frac;
    if (frac < 100) return whole + ".00" + frac;
    if (frac < 1000) return whole + ".0" + frac;
    return whole + "." + frac;
  }

  private static double calibrate(double probability) {
    return probability;
  }

  private static double score(String body) {
    String requestedAt = jsonString(body, "\"requested_at\"", "");
    String lastTs = jsonString(body, "\"timestamp\"", "");
    int merchantPos = body.indexOf("\"merchant\"");
    String merchantObj = merchantPos >= 0 ? body.substring(merchantPos) : "";
    String merchantId = jsonString(merchantObj, "\"id\"", "");
    String mcc = jsonString(merchantObj, "\"mcc\"", "");

    double amount = jsonNumber(body, "\"amount\"", 0);
    double installments = jsonNumber(body, "\"installments\"", 0);
    double customerAvg = jsonNumber(body, "\"avg_amount\"", 0.01);
    double txCount = jsonNumber(body, "\"tx_count_24h\"", 0);
    double kmHome = jsonNumber(body, "\"km_from_home\"", 0);
    double kmLast = jsonNumber(body, "\"km_from_current\"", 0);
    double merchantAvg = jsonNumber(merchantObj, "\"avg_amount\"", 0);
    boolean hasLast = !body.contains("\"last_transaction\":null") && !lastTs.isEmpty();

    double z = W[0];
    z += W[1] * clamp(amount / 10000.0);
    z += W[2] * clamp(installments / 12.0);
    z += W[3] * clamp((amount / (customerAvg > 0 ? customerAvg : 0.01)) / 10.0);
    z += W[4] * (isoHour(requestedAt) / 23.0);
    z += W[5] * (mondayBasedDow(requestedAt) / 6.0);
    z += W[6] * (hasLast ? clamp(minutesDelta(requestedAt, lastTs) / 1440.0) : -1.0);
    z += W[7] * (hasLast ? clamp(kmLast / 1000.0) : -1.0);
    z += W[8] * clamp(kmHome / 1000.0);
    z += W[9] * clamp(txCount / 20.0);
    z += W[10] * (jsonBoolean(body, "\"is_online\"") ? 1.0 : 0.0);
    z += W[11] * (jsonBoolean(body, "\"card_present\"") ? 1.0 : 0.0);
    z += W[12] * (merchantUnknown(body, merchantId) ? 1.0 : 0.0);
    z += W[13] * mccRisk(mcc);
    z += W[14] * clamp(merchantAvg / 10000.0);
    z += W[15] * (hasLast ? 1.0 : 0.0);
    z += W[16] * (Math.log1p(amount / (customerAvg > 0 ? customerAvg : 0.01)) / Math.log(101.0));
    z += W[17] * (Math.log1p(kmHome) / Math.log(1001.0));
    z += W[18] * (Math.log1p(kmLast) / Math.log(1001.0));

    if (z > 30) return 1.0;
    if (z < -30) return 0.0;
    return 1.0 / (1.0 + Math.exp(-z));
  }

  private static double clamp(double x) {
    return x < 0 ? 0 : Math.min(x, 1);
  }

  private static double jsonNumber(String s, String key, double fallback) {
    int p = s.indexOf(key);
    if (p < 0) return fallback;
    p += key.length();
    while (p < s.length() && (s.charAt(p) == ' ' || s.charAt(p) == '\n' || s.charAt(p) == '\r'
        || s.charAt(p) == '\t' || s.charAt(p) == ':')) p++;
    int e = p;
    while (e < s.length()) {
      char c = s.charAt(e);
      if (!(c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || (c >= '0' && c <= '9'))) break;
      e++;
    }
    try {
      return Double.parseDouble(s.substring(p, e));
    } catch (Exception ignored) {
      return fallback;
    }
  }

  private static boolean jsonBoolean(String s, String key) {
    int p = s.indexOf(key);
    if (p < 0) return false;
    p += key.length();
    while (p < s.length() && (s.charAt(p) == ' ' || s.charAt(p) == '\n' || s.charAt(p) == '\r'
        || s.charAt(p) == '\t' || s.charAt(p) == ':')) p++;
    return p + 4 <= s.length() && s.startsWith("true", p);
  }

  private static String jsonString(String s, String key, String fallback) {
    int p = s.indexOf(key);
    if (p < 0) return fallback;
    p += key.length();
    while (p < s.length() && s.charAt(p) != '"') p++;
    if (p >= s.length()) return fallback;
    int start = ++p;
    while (p < s.length() && s.charAt(p) != '"') p++;
    return p > start ? s.substring(start, p) : fallback;
  }

  private static boolean merchantUnknown(String body, String merchantId) {
    int k = body.indexOf("\"known_merchants\"");
    int m = body.indexOf("\"merchant\"");
    if (k < 0 || m < 0 || k > m || merchantId.isEmpty()) return true;
    return !body.substring(k, m).contains(merchantId);
  }

  private static int isoHour(String iso) {
    return iso.length() >= 13 ? (iso.charAt(11) - '0') * 10 + (iso.charAt(12) - '0') : 0;
  }

  private static int isoMinuteOfDay(String iso) {
    if (iso.length() < 16) return 0;
    int hour = (iso.charAt(11) - '0') * 10 + (iso.charAt(12) - '0');
    int min = (iso.charAt(14) - '0') * 10 + (iso.charAt(15) - '0');
    return hour * 60 + min;
  }

  private static double minutesDelta(String current, String previous) {
    return (daysFromIso(current) - daysFromIso(previous)) * 1440.0
        + isoMinuteOfDay(current) - isoMinuteOfDay(previous);
  }

  private static int mondayBasedDow(String iso) {
    int dow = (daysFromIso(iso) + 3) % 7;
    return dow < 0 ? dow + 7 : dow;
  }

  private static int daysFromIso(String iso) {
    if (iso.length() < 10) return 0;
    int y = Integer.parseInt(iso.substring(0, 4));
    int m = Integer.parseInt(iso.substring(5, 7));
    int d = Integer.parseInt(iso.substring(8, 10));
    y -= m <= 2 ? 1 : 0;
    int era = Math.floorDiv(y, 400);
    int yoe = y - era * 400;
    int mp = m + (m > 2 ? -3 : 9);
    int doy = (153 * mp + 2) / 5 + d - 1;
    int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    return era * 146097 + doe - 719468;
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
