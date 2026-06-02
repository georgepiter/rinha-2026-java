package br.com.rinha.server;

public final class Main {

    public static void main(String[] args) throws Exception {
        new RinhaHttpServer(8080).start();
    }
}
