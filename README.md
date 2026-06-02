# Rinha de Backend 2026 - Java

Implementacao em Java 21 puro para a Rinha de Backend 2026.

## Stack

- Java 21
- Nginx
- Docker Compose
- Sem framework e sem dependencias externas

## Estrutura

- `server/Main.java`: ponto de entrada da aplicacao.
- `server/RinhaHttpServer.java`: servidor HTTP com `ServerSocket` e virtual threads.
- `controller/FraudController.java`: rotas `/ready` e `/fraud-score`.
- `service/FraudScorer.java`: motor de decisao antifraude.
- `util/`: leitura HTTP, parser JSON manual, datas e formatacao numerica.
- `nginx.conf`: load balancer com keepalive para as duas APIs.
- `docker-compose.yml`: duas instancias Java atras do Nginx.

## Como rodar

```bash
docker compose up --build
```

Healthcheck:

```text
GET http://localhost:9999/ready
```

Endpoint principal:

```text
POST http://localhost:9999/fraud-score
```

## Como testar

Com a stack rodando, execute os testes oficiais em outro terminal:

```bash
cd sua pasta\test
k6 run smoke.js
k6 run test.js
```

O resultado fica em:

```text
sua pasta\test\results.json
```

## Resultado local

Rodada local em 2026-06-02 usando o teste oficial:

- Smoke oficial: 100% dos checks, 0 erro HTTP.
- Teste completo: 54.100 requisicoes processadas, 0 erro HTTP.
- p99: 1.14 ms.
- Falhas ponderadas: 1241.
- Failure rate: 2.29%.
- Score final: 3652.68.

