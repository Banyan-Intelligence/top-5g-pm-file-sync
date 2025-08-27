package com.tejas.pmfilesync5g.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "")
@Data
public class TopicConfiguration {
    
    private List<TopicConfig> topics;
    private GlobalConsumer globalConsumer;
    private GlobalProducer globalProducer;
    private SecurityConfig security;
    private TopicManagement topicManagement;
    private ErrorHandling errorHandling;
    private Monitoring monitoring;
    
    @Data
    public static class TopicConfig {
        private String name;
        private String broker;
        private int partitions;
        private ConsumerConfig consumer;
        private String description;
    }
    
    @Data
    public static class ConsumerConfig {
        private String groupId;
        private int threads;
        private String autoOffsetReset;
    }
    
    @Data
    public static class GlobalConsumer {
        private String keyDeserializer;
        private String valueDeserializer;
        private boolean enableAutoCommit;
        private int autoCommitIntervalMs;
        private int sessionTimeoutMs;
        private int maxPollRecords;
    }
    
    @Data
    public static class GlobalProducer {
        private String keySerializer;
        private String valueSerializer;
        private String acks;
        private int retries;
        private int batchSize;
        private int lingerMs;
        private int bufferMemory;
    }
    
    @Data
    public static class SecurityConfig {
        private String protocol;
        private SaslConfig sasl;
    }
    
    @Data
    public static class SaslConfig {
        private String mechanism;
        private String jaasConfig;
    }
    
    @Data
    public static class TopicManagement {
        private boolean autoCreateTopics;
        private boolean autoDeleteTopics;
        private int topicCreationTimeout;
        private int topicDeletionTimeout;
    }
    
    @Data
    public static class ErrorHandling {
        private RetryConfig retry;
        private DeadLetterTopic deadLetterTopic;
        private CircuitBreaker circuitBreaker;
    }
    
    @Data
    public static class RetryConfig {
        private int maxAttempts;
        private int backoffDelay;
        private double backoffMultiplier;
    }
    
    @Data
    public static class DeadLetterTopic {
        private boolean enabled;
        private String suffix;
    }
    
    @Data
    public static class CircuitBreaker {
        private boolean enabled;
        private int failureThreshold;
        private int timeout;
    }
    
    @Data
    public static class Monitoring {
        private HealthCheck healthCheck;
        private Metrics metrics;
    }
    
    @Data
    public static class HealthCheck {
        private boolean enabled;
        private int interval;
        private int timeout;
    }
    
    @Data
    public static class Metrics {
        private boolean enabled;
        private boolean jmxEnabled;
        private int jmxPort;
    }
} 