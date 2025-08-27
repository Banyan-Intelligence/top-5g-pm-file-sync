# 📡 TOP 5G PM File Sync

A high-performance Spring Boot microservice for processing 5G Performance Management (PM) files from VES (Virtualized Event Streaming) events, with dynamic Kafka consumption, SFTP integration, and TimescaleDB ingestion.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)
[![TimescaleDB](https://img.shields.io/badge/TimescaleDB-Supported-success.svg)](https://www.timescale.com/)

## 🚀 Overview

The **TOP 5G PM File Sync** microservice is designed to handle real-time processing of 5G network performance management data. It consumes VES events from Kafka topics, processes tar file locations via SFTP, extracts performance metrics from XML files, and ingests structured data into TimescaleDB for time-series analysis.

### Key Features

- ✅ **Dynamic Kafka Consumer Configuration** - Configurable multi-threaded consumption from multiple topics
- ✅ **VES Event Processing** - Full VES 7.1.1 specification compliance with JSON deserialization
- ✅ **SFTP Integration** - Secure file transfer protocol support with JSch library
- ✅ **TimescaleDB Ingestion** - Optimized time-series database storage with automatic partitioning  
- ✅ **Multi-tenant Support** - Separate processing for DU (Distributed Unit) and CU (Centralized Unit) data
- ✅ **Health Monitoring** - Comprehensive health checks with processing metrics
- ✅ **Docker Orchestration** - Complete containerized environment with docker-compose
- ✅ **Retry Mechanisms** - Exponential backoff and circuit breaker patterns
- ✅ **Performance Optimization** - Parallel processing with configurable thread pools

## 🏗️ Architecture

```mermaid
graph TB
    A[VES Events] --> B[Kafka Topics]
    B --> C[Dynamic Kafka Consumers]
    C --> D[Message Processor Service]
    D --> E[VES Event Parser]
    E --> F[SFTP Service]
    F --> G[File Ingestion Service]
    G --> H[TimescaleDB]
    
    subgraph "Kafka Topics"
        B1[top.5g.pm.files.test1]
        B2[top.5g.pm.files.test2]
    end
    
    subgraph "Database Tables"
        H1[du_pm_file_sync]
        H2[cu_pm_file_sync]
    end
    
    C --> B1
    C --> B2
    G --> H1
    G --> H2
```

## 🔧 Technical Specifications

| Component | Technology | Version |
|-----------|------------|---------|
| **Runtime** | Java OpenJDK | 21 |
| **Framework** | Spring Boot | 3.4.2 |
| **Message Queue** | Apache Kafka | 2.13-2.8.1 |
| **Database** | TimescaleDB | 2.11.0-pg15 |
| **File Transfer** | SFTP (JSch) | 0.1.55 |
| **Containerization** | Docker | Latest |
| **Build Tool** | Maven | 3.8+ |

### Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>com.jcraft</groupId>
        <artifactId>jsch</artifactId>
        <version>0.1.55</version>
    </dependency>
</dependencies>
```

## 📦 Installation & Setup

### Prerequisites

- **Java 21** or higher
- **Docker** and **Docker Compose**
- **Maven 3.8+** for building from source
- **Git** for version control

### Quick Start with Docker

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd TOP/top-5g-pm-file-sync
   ```

2. **Build the Application**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Start the Complete Environment**
   ```bash
   cd ../test-environment
   sudo docker-compose up -d
   ```

4. **Verify Services**
   ```bash
   curl http://localhost:8520/api/v1/health
   ```

### Manual Installation

1. **Build from Source**
   ```bash
   mvn clean install
   ```

2. **Configure Application**
   ```bash
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   # Edit configuration as needed
   ```

3. **Run Application**
   ```bash
   java -jar target/top-5g-pm-file-sync-1.0.0.jar
   ```

## ⚙️ Configuration

### Core Configuration (`application.yml`)

```yaml
server:
  port: 8520

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: top-5g-pm-file-sync
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/pm_file_sync}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect

app:
  sftp:
    timeout: 30000
    retry:
      maxAttempts: 3
      backoffMs: 1000
```

### Kafka Topics Configuration (`topic_config.yml`)

```yaml
kafka_brokers:
  - broker: kafka:29092
    topics:
      - name: top.5g.pm.files.test1
        consumer_threads: 4
      - name: top.5g.pm.files.test2
        consumer_threads: 4
```

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | localhost:9092 | ✅ |
| `DATABASE_URL` | TimescaleDB connection URL | jdbc:postgresql://localhost:5432/pm_file_sync | ✅ |
| `DATABASE_USERNAME` | Database username | postgres | ✅ |
| `DATABASE_PASSWORD` | Database password | postgres | ✅ |
| `SFTP_TIMEOUT` | SFTP connection timeout (ms) | 30000 | ❌ |
| `RETRY_MAX_ATTEMPTS` | Maximum retry attempts | 3 | ❌ |

## 🔄 Usage & Testing

### VES Event Format

The service processes VES events in the following format:

```json
{
  "event": {
    "commonEventHeader": {
      "domain": "notification",
      "version": "4.1",
      "eventId": "Notification_DU_PM_1",
      "eventName": "Notification_Performance",
      "sourceId": "00256D",
      "reportingEntityName": "Tejas-gNB-RAC-NR-151-1",
      "sourceName": "Tejas-gNB-RAC-NR-151-1",
      "eventType": "kpi_Performance_notification"
    },
    "notificationFields": {
      "changeIdentifier": "PM_MEAS_FILES",
      "changeType": "fileUploaded",
      "arrayOfNamedHashMap": [{
        "name": "RAC-NR-151_du_pm_logs_20250827_180005.tar",
        "hashMap": {
          "location": "sftp://root:swtn100tj@sftp-server:22/var/data/O-RAN/O-DU/PM/RAC-NR-151_du_pm_logs_20250827_180005.tar",
          "compression": "tar",
          "fileFormatType": "TAR",
          "fileSize": "10240",
          "fileDataType": "PERFORMANCE"
        }
      }]
    }
  }
}
```

### Testing Commands

**Send Test Messages:**
```bash
cd test-environment
./send-messages-fixed.sh
```

**Check Database Records:**
```bash
# DU Records
sudo docker exec timescaledb psql -U postgres -d pm_file_sync -c \
  "SELECT id, serial_number, file_path, status FROM du_pm_file_sync ORDER BY created_at;"

# CU Records  
sudo docker exec timescaledb psql -U postgres -d pm_file_sync -c \
  "SELECT id, serial_number, file_path, status FROM cu_pm_file_sync ORDER BY created_at;"
```

**Monitor Processing:**
```bash
sudo docker logs -f top-5g-pm-file-sync
```

### Expected Results

After processing, you should see:
- **6 database records** total (3 DU + 3 CU)
- **Unique UUIDs** for each record
- **Serial numbers** extracted from VES source names
- **File paths** with proper DU/CU classification
- **Status 0** (CREATED) for all records

## 📊 API Documentation

### Health Check Endpoint

**GET** `/api/v1/health`

Returns service health and processing metrics:

```json
{
  "service": "top-5g-pm-file-sync",
  "status": "UP",
  "timestamp": 1756328677858,
  "kafka": {
    "status": "UP",
    "totalMessagesProcessed": 2,
    "processingErrors": 0
  }
}
```

### File Sync Controller

**GET** `/api/v1/file-sync/du/status`
- Returns DU file sync status and statistics

**GET** `/api/v1/file-sync/cu/status`
- Returns CU file sync status and statistics

**POST** `/api/v1/file-sync/reprocess/{id}`
- Triggers reprocessing of a specific file record

## 🛠️ Development Guide

### Project Structure

```
top-5g-pm-file-sync/
├── src/main/java/com/tejas/pmfilesync5g/
│   ├── config/           # Configuration classes
│   │   ├── KafkaConsumerConfig.java
│   │   └── ObjectMapperConfig.java
│   ├── consumer/         # Kafka message consumers
│   │   └── DynamicKafkaConsumer.java
│   ├── controller/       # REST API controllers
│   │   ├── HealthController.java
│   │   └── FileSyncController.java
│   ├── dto/             # Data Transfer Objects
│   │   └── VesEvent.java
│   ├── entity/          # JPA Entities
│   │   ├── DuPmFileSync.java
│   │   └── CuPmFileSync.java
│   ├── repository/      # Data repositories
│   │   ├── DuPmFileSyncRepository.java
│   │   └── CuPmFileSyncRepository.java
│   └── service/         # Business logic services
│       ├── MessageProcessorService.java
│       ├── SftpService.java
│       └── FileIngestionService.java
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql
├── Dockerfile
├── pom.xml
└── README.md
```

### Building and Testing

**Compile and Package:**
```bash
mvn clean compile
mvn package -DskipTests
```


### Adding New Features

1. **Fork the repository** and create a feature branch
2. **Implement changes** following Spring Boot best practices
3. **Add unit tests** with minimum 80% coverage
4. **Update documentation** including this README
5. **Submit a pull request** with detailed description

## 📈 Monitoring & Observability

### Health Monitoring

The service exposes several health indicators:

- **Kafka Consumer Health**: Monitors consumer group status and lag
- **Database Health**: Checks TimescaleDB connectivity
- **SFTP Health**: Validates SFTP server accessibility
- **Processing Metrics**: Tracks message throughput and error rates

### Logging Configuration

```yaml
logging:
  level:
    com.tejas.pmfilesync5g: INFO
    org.springframework.kafka: WARN
    org.hibernate: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Metrics Export

Enable Micrometer metrics for monitoring integration:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## 🔍 Troubleshooting

### Common Issues

**1. Kafka Connection Issues**
```bash
# Check Kafka broker connectivity
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

**2. Database Connection Problems**
```bash
# Test database connection
docker exec timescaledb psql -U postgres -d pm_file_sync -c "\dt"
```

**3. SFTP Authentication Failures**
```bash
# Verify SFTP server access
docker exec top-5g-pm-file-sync sftp root@sftp-server
```

**4. Message Processing Delays**
- Check consumer group lag: `kafka-consumer-groups --bootstrap-server localhost:9092 --group top-5g-pm-file-sync --describe`
- Monitor application logs: `docker logs -f top-5g-pm-file-sync`
- Verify database performance: Check TimescaleDB query execution times

### Debug Mode

Enable debug logging:
```yaml
logging:
  level:
    com.tejas.pmfilesync5g: DEBUG
    org.springframework.kafka.listener: DEBUG
```

### Performance Tuning

**Kafka Consumer Optimization:**
```yaml
spring:
  kafka:
    consumer:
      fetch-min-size: 1048576
      fetch-max-wait: 500ms
      max-poll-records: 500
```

**Database Connection Pool:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
```

## 🚀 Production Deployment

### Resource Requirements

**Minimum Requirements:**
- CPU: 2 cores
- Memory: 4GB RAM
- Storage: 20GB
- Network: 1Gbps

**Recommended for Production:**
- CPU: 4+ cores
- Memory: 8GB+ RAM
- Storage: 100GB+ SSD
- Network: 10Gbps

### Docker Compose Production

```yaml
version: '3.8'
services:
  top-5g-pm-file-sync:
    image: top-5g-pm-file-sync:latest
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G
    environment:
      - JAVA_OPTS=-Xmx3g -Xms2g
      - SPRING_PROFILES_ACTIVE=production
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: top-5g-pm-file-sync
spec:
  replicas: 3
  selector:
    matchLabels:
      app: top-5g-pm-file-sync
  template:
    spec:
      containers:
      - name: app
        image: top-5g-pm-file-sync:latest
        resources:
          requests:
            memory: "2Gi"
            cpu: "1"
          limits:
            memory: "4Gi"
            cpu: "2"
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.


---
*For questions or support, please contact the development team or create an issue in this repository.*