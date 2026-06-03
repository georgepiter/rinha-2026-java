# Rinha de Backend 2026 - Java

Implementacao em Java 21 puro para a Rinha de Backend 2026.

Projeto sem framework, com servidor HTTP proprio, parser JSON manual e duas
instancias Java atras de um Nginx.

## Stack

- Java 21
- Nginx
- Docker Compose
- Sem framework e sem dependencias externas

## Estrutura

- `src/main/java/br/com/rinha/server/Main.java`: ponto de entrada da aplicacao.
- `src/main/java/br/com/rinha/server/RinhaHttpServer.java`: servidor HTTP com `ServerSocket` e virtual threads.
- `src/main/java/br/com/rinha/controller/FraudController.java`: rotas `/ready` e `/fraud-score`.
- `src/main/java/br/com/rinha/service/FraudScorer.java`: motor de decisao antifraude.
- `src/main/java/br/com/rinha/util/HttpRequestReader.java`: leitura de requisicoes HTTP.
- `src/main/java/br/com/rinha/util/FastJson.java`: parser JSON manual para o hot path.
- `src/main/java/br/com/rinha/util/DateMath.java`: calculos de data sem alocacoes extras.
- `src/main/java/br/com/rinha/util/NumberFormatter.java`: formatacao do `fraud_score`.
- `Dockerfile`: build multi-stage com JDK/JRE Alpine.
- `nginx.conf`: load balancer com keepalive para as duas APIs.
- `docker-compose.yml`: duas instancias Java atras do Nginx.
- `info.json`: metadados da submissao.

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

## Submissao

A branch `submission` usa imagem publicada no GHCR com tag fixa por commit.
Isso evita cache de `latest` durante a previa oficial.

Imagem atual da submissao:

```text
ghcr.io/georgepiter/rinha-2026-java:balanced-4c876da
```

Fluxo usado para publicar uma nova versao:

```bash
git push origin main
docker build -t ghcr.io/georgepiter/rinha-2026-java:<tag> .
docker push ghcr.io/georgepiter/rinha-2026-java:<tag>
git switch submission
# atualizar docker-compose.yml para a nova tag
git push origin submission
```

## Resultado local

Rodada local em 2026-06-03 usando o teste oficial:

- Smoke oficial: 100% dos checks, 0 erro HTTP.
- Teste completo: 54.100 requisicoes processadas, 0 erro HTTP.
- p99: 1.09 ms.
- Falsos positivos: 1238.
- Falsos negativos: 1.
- Falhas ponderadas: 1241.
- Failure rate: 2.29%.
- Score final: 3672.66.

