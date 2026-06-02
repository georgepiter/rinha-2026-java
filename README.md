# Rinha de Backend 2026 - Java

Implementacao em Java 21 puro para a Rinha de Backend 2026.

## Estrutura

- `src/main/java/br/com/rinha/server/Main.java`: servidor HTTP manual com `ServerSocket`, virtual threads e parser JSON direto para reduzir overhead.
- `Dockerfile`: build multi-stage com Eclipse Temurin 21 Alpine.
- `docker-compose.yml`: duas APIs Java atras de Nginx, respeitando o limite total de CPU/memoria.
- `nginx.conf`: balanceamento com keepalive para as duas APIs.

## Como rodar

```bash
docker compose up --build
```

Endpoint principal:

```text
POST http://localhost:9999/fraud-score
```

Healthcheck:

```text
GET http://localhost:9999/ready
```

## Validacao local

Rodada local em 2026-06-02 usando o teste oficial em `D:\Projetos\test`:

- Smoke oficial: 100% dos checks, 0 erro HTTP.
- Teste completo: 54.100 requisicoes processadas, 0 erro HTTP.
- p99: 1.26 ms.
- Falhas ponderadas: 1241.
- Failure rate: 2.29%.
- Score final: 3610.42.

Observacao: o resultado pode variar conforme Docker Desktop, CPU disponivel e outros processos rodando na maquina.
