package com.tejas.pmfilesync5g.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PmFileProcessorService {

  public void processMessage(String message) {
    // TODO: Implement domain-specific parsing and persistence
    log.info("Processing PM file message: {}", message);
  }
} 