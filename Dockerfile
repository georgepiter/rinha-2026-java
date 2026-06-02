FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app
COPY src ./src
RUN find src -name "*.java" > sources.txt && javac -d /app/bin @sources.txt

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/bin ./bin
EXPOSE 8080

CMD ["java", "-Xms16m", "-Xmx64m", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75", "-cp", "bin", "br.com.rinha.server.Main"]
