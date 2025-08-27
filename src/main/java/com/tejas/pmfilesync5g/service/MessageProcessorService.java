package com.tejas.pmfilesync5g.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tejas.pmfilesync5g.dto.VesEvent;
import com.tejas.pmfilesync5g.exception.MessageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@EnableRetry
@RequiredArgsConstructor
public class MessageProcessorService {

    private final ObjectMapper objectMapper;
    private final FileIngestionService fileIngestionService;

    @Value("${app.kafka.error-handling.max-retries:3}")
    private int maxRetries;

    @Value("${app.kafka.error-handling.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);
    private final AtomicLong processingErrors = new AtomicLong(0);

    @Retryable(retryFor = {MessageProcessingException.class}, 
               maxAttempts = 3,
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public void processMessage(String message, String topicName) {
        try {
            log.debug("Processing VES event message from topic: {}", topicName);
            
            VesEvent vesEvent = objectMapper.readValue(message, VesEvent.class);
            
            if (vesEvent.getEvent() == null || vesEvent.getEvent().getNotificationFields() == null) {
                log.warn("Invalid VES event structure, missing event or notificationFields");
                return;
            }
            
            String sourceName = vesEvent.getEvent().getCommonEventHeader().getSourceName();
            UUID rsyncId = UUID.randomUUID();
            
            var notificationFields = vesEvent.getEvent().getNotificationFields();
            if (notificationFields.getArrayOfNamedHashMap() != null) {
                for (var namedHashMap : notificationFields.getArrayOfNamedHashMap()) {
                    if (namedHashMap.getHashMap() != null) {
                        String location = namedHashMap.getHashMap().getLocation();
                        if (location != null && !location.isEmpty()) {
                            log.info("Processing file location: {} from source: {}", location, sourceName);
                            fileIngestionService.processVesEvent(location, sourceName, rsyncId);
                        }
                    }
                }
            }
            
            totalMessagesProcessed.incrementAndGet();
            
        } catch (Exception e) {
            processingErrors.incrementAndGet();
            log.error("Error processing VES event message from topic: {} (attempt will be retried)", topicName, e);
            throw new MessageProcessingException("Failed to process VES event message from topic: " + topicName, e);
        }
    }

    public long getTotalMessagesProcessed() {
        return totalMessagesProcessed.get();
    }

    public long getProcessingErrors() {
        return processingErrors.get();
    }
}