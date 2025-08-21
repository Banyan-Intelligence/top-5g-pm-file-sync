# top-5g-pm-file-sync

A Spring Boot 3 (Java 21) microservice that consumes Kafka messages for 5G PM file processing. Mirrors structure and conventions from the 4G repos.

## Features
- Spring Kafka consumer with configurable topic and credentials via env file
- Actuator for health/metrics
- Dockerfile and docker-compose for containerized runs

## Requirements
- Java 21, Maven 3.9+
- Docker (optional, for container run)
- Kafka cluster reachable from the app container/host

## Configuration
All runtime configuration should be set in the env file `top-5g-pm-file-sync.env`.

Key variables:
- `APP_PORT` (default 8520)
- `APP_CONTEXT_PATH` (default /api/v1)
- `KAFKA_BOOTSTRAP_SERVERS` (e.g., localhost:9092)
- `KAFKA_GROUP_ID` (default top-5g-pm-file-sync)
- `KAFKA_TOPIC` (e.g., top.5g.pm.files)
- `KAFKA_AUTO_OFFSET_RESET` (default latest)
- `KAFKA_SECURITY_PROTOCOL` (PLAINTEXT, SASL_PLAINTEXT, SASL_SSL)
- `KAFKA_SASL_MECHANISM` (PLAIN, SCRAM-SHA-256/512)
- `KAFKA_SASL_JAAS_CONFIG` (optional JAAS config line)

## Build
```
mvn clean package -DskipTests
```
The fat jar will be at `target/top-5g-pm-file-sync-*.jar`.

## Run (Local)
```
export $(grep -v '^#' top-5g-pm-file-sync.env | xargs)
java -jar target/top-5g-pm-file-sync-*.jar
```

## Run (Docker Compose)
Assumes the external Docker network `top` exists and Kafka is already running.
```
docker network create top || true
docker compose up --build -d
```

Logs are written to `/app/logs` inside the container and mapped to `${HOME}/logs/top-5g-pm-file-sync` on the host.

## Healthcheck
Actuator health: `http://localhost:8520/actuator/health` 