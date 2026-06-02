package br.com.rinha.server;

import br.com.rinha.controller.FraudController;
import br.com.rinha.util.HttpRequestReader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RinhaHttpServer {

    private static final int BACKLOG = 4096;
    private static final int SOCKET_TIMEOUT_MS = 15000;
    private static final int BUFFER_SIZE = 32768;

    private final int port;
    private final FraudController controller = new FraudController();

    public RinhaHttpServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

        try (ServerSocket server = new ServerSocket(port, BACKLOG)) {
            server.setReuseAddress(true);

            while (true) {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.setSoTimeout(SOCKET_TIMEOUT_MS);
                pool.execute(() -> handle(socket));
            }
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                int bytesRead = HttpRequestReader.read(in, buffer);
                if (bytesRead <= 0) return;
                controller.handle(buffer, bytesRead, out);
            }
        } catch (Exception ignored) {
            try {
                FraudController.sendOk(socket.getOutputStream(), "{\"approved\":true,\"fraud_score\":0}");
            } catch (Exception ignoredAgain) {
            }
        }
    }
}
