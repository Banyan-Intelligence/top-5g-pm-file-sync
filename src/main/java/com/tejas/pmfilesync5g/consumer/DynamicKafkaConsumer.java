package com.tejas.pmfilesync5g.consumer;

import com.tejas.pmfilesync5g.config.KafkaConsumerConfig;
import com.tejas.pmfilesync5g.config.TopicConfiguration;
import com.tejas.pmfilesync5g.service.MessageProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicKafkaConsumer {

    private final TopicConfiguration topicConfiguration;
    private final MessageProcessorService messageProcessorService;
    private final KafkaConsumerConfig kafkaConsumerConfig;
    
    private final Map<String, MessageListenerContainer> activeContainers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeConsumers() {
        log.info("Initializing dynamic Kafka consumers from topic configuration...");
        
        List<TopicConfiguration.TopicConfig> topics = topicConfiguration.getTopics();
        if (topics == null || topics.isEmpty()) {
            log.warn("No topics configured for consumer initialization");
            return;
        }
        
        log.info("Found {} topics to configure", topics.size());
        
        for (TopicConfiguration.TopicConfig topic : topics) {
            createConsumerForTopic(topic);
        }
        
        log.info("Successfully initialized {} dynamic consumers", activeContainers.size());
    }
    
    private void createConsumerForTopic(TopicConfiguration.TopicConfig topic) {
        try {
            log.info("Creating consumer for topic: {} with {} threads on broker: {}", 
                topic.getName(), topic.getConsumer().getThreads(), topic.getBroker());
            
            ConsumerFactory<String, String> consumerFactory = kafkaConsumerConfig.createConsumerFactory(
                topic.getBroker(), topic.getConsumer().getGroupId());
            
            ContainerProperties containerProps = new ContainerProperties(topic.getName());
            containerProps.setGroupId(topic.getConsumer().getGroupId());
            containerProps.setClientId(topic.getName() + "-consumer");
            
            ConcurrentMessageListenerContainer<String, String> container = 
                new ConcurrentMessageListenerContainer<>(consumerFactory, containerProps);
            
            container.setConcurrency(topic.getConsumer().getThreads());
            
            container.setupMessageListener(new MessageListener<String, String>() {
                @Override
                public void onMessage(ConsumerRecord<String, String> record) {
                    processMessage(record.value(), record.topic());
                }
            });
            
            container.start();
            activeContainers.put(topic.getName(), container);
            
            log.info("Successfully created and started consumer for topic: {}", topic.getName());
            
        } catch (Exception e) {
            log.error("Failed to create consumer for topic: {}", topic.getName(), e);
        }
    }
    
    private void processMessage(String message, String topicName) {
        if (message == null || message.isEmpty()) {
            log.warn("Received empty message from topic: {}, skipping", topicName);
            return;
        }
        
        try {
            log.debug("Processing message from topic: {}", topicName);
            messageProcessorService.processMessage(message, topicName);
        } catch (Exception ex) {
            log.error("Error processing message from topic: {}", topicName, ex);
        }
    }
    
    @PreDestroy
    public void stopAllConsumers() {
        log.info("Stopping all dynamic consumers...");
        activeContainers.values().forEach(container -> {
            try {
                container.stop();
            } catch (Exception e) {
                log.error("Error stopping consumer container", e);
            }
        });
        activeContainers.clear();
        log.info("All consumers stopped");
    }
}