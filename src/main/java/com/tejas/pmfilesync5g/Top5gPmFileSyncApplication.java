package com.tejas.pmfilesync5g;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class Top5gPmFileSyncApplication {

  public static void main(String[] args) {
    SpringApplication.run(Top5gPmFileSyncApplication.class, args);
  }
} 