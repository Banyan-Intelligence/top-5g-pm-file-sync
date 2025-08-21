package com.tejas.pmfilesync5g.consumer;

import com.tejas.pmfilesync5g.service.PmFileProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PmFileEventListener {

  private final PmFileProcessorService pmFileProcessorService;

  @KafkaListener(
      topics = "${app.kafka.topic}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void onMessage(@Payload String message) {
    if (message == null || message.isEmpty()) {
      log.warn("Received empty Kafka message, skipping");
      return;
    }
    try {
      log.debug("Received Kafka message: {}", message);
      pmFileProcessorService.processMessage(message);
    } catch (Exception ex) {
      log.error("Error while processing Kafka message", ex);
      // Let the container handle retries based on consumer config
    }
  }
} 