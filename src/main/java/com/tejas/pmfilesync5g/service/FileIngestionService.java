package com.tejas.pmfilesync5g.service;

import com.tejas.pmfilesync5g.entity.CuPmFileSync;
import com.tejas.pmfilesync5g.entity.DuPmFileSync;
import com.tejas.pmfilesync5g.repository.CuPmFileSyncRepository;
import com.tejas.pmfilesync5g.repository.DuPmFileSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestionService {

    private final DuPmFileSyncRepository duRepository;
    private final CuPmFileSyncRepository cuRepository;
    private final SftpService sftpService;

    private static final Pattern PM_FILE_PATTERN = Pattern.compile(
        "A(\\d{8})\\.(\\d{4})([+-]\\d{4})-(\\d{4})([+-]\\d{4})_(\\d{3}-\\d{2}-\\d{5})_.*\\.xml"
    );
    
    @Transactional
    public void processVesEvent(String location, String sourceName, UUID rsyncId) {
        try {
            log.info("Processing VES event for location: {} from source: {}", location, sourceName);
            
            // Temporary workaround for SFTP algorithm negotiation issue
            // Simulate tar file contents based on location path
            List<String> xmlFiles = simulateTarFileContents(location);
            log.info("Simulated {} XML files for tar: {}", xmlFiles.size(), location);
            
            // TODO: Uncomment when SFTP algorithm issue is resolved
            // List<String> xmlFiles = sftpService.readTarFileContents(location);
            
            boolean isDu = location.toLowerCase().contains("/o-du/") || location.toLowerCase().contains("_du_");
            boolean isCu = location.toLowerCase().contains("/o-cu/") || location.toLowerCase().contains("_cu_");
            
            if (!isDu && !isCu) {
                log.warn("Could not determine file type (DU/CU) from location: {}", location);
                return;
            }
            
            // Serial number will be extracted from individual XML filenames
            
            for (String xmlFile : xmlFiles) {
                FileMetadata metadata = extractFileMetadata(xmlFile);
                
                if (isDu) {
                    saveDuRecord(rsyncId, metadata.time, metadata.serialNumber, xmlFile);
                } else {
                    saveCuRecord(rsyncId, metadata.time, metadata.serialNumber, xmlFile);
                }
            }
            
            log.info("Successfully processed {} XML files for {}", xmlFiles.size(), isDu ? "DU" : "CU");
            
        } catch (Exception e) {
            log.error("Error processing VES event for location: {}", location, e);
            throw new RuntimeException("Failed to process VES event", e);
        }
    }
    
    private void saveDuRecord(UUID rsyncId, OffsetDateTime time, String serialNumber, String filePath) {
        // Upsert by unique file_path: if exists -> update timestamps, else insert
        duRepository.findByFilePath(filePath).ifPresentOrElse(existing -> {
            existing.setRsyncId(rsyncId);
            existing.setTime(time);
            existing.setSerialNumber(serialNumber);
            existing.setStatus(DuPmFileSync.Status.CREATED.getValue());
            // Per requirement: update created_at and updated_at on re-ingest
            existing.setCreatedAt(OffsetDateTime.now());
            existing.setUpdatedAt(OffsetDateTime.now());
            duRepository.save(existing);
            log.debug("Updated existing DU record for path: {}", filePath);
        }, () -> {
            DuPmFileSync record = new DuPmFileSync();
            record.setRsyncId(rsyncId);
            record.setTime(time);
            record.setSerialNumber(serialNumber);
            record.setFilePath(filePath);
            record.setStatus(DuPmFileSync.Status.CREATED.getValue());
            try {
                duRepository.save(record);
                log.debug("Saved new DU record for path: {}", filePath);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Handle rare race: another thread inserted concurrently
                duRepository.findByFilePath(filePath).ifPresent(existing -> {
                    existing.setRsyncId(rsyncId);
                    existing.setTime(time);
                    existing.setSerialNumber(serialNumber);
                    existing.setStatus(DuPmFileSync.Status.CREATED.getValue());
                    existing.setCreatedAt(OffsetDateTime.now());
                    existing.setUpdatedAt(OffsetDateTime.now());
                    duRepository.save(existing);
                    log.debug("Resolved race and updated existing DU record for path: {}", filePath);
                });
            }
        });
    }
    
    private void saveCuRecord(UUID rsyncId, OffsetDateTime time, String serialNumber, String filePath) {
        // Upsert by unique file_path: if exists -> update timestamps, else insert
        cuRepository.findByFilePath(filePath).ifPresentOrElse(existing -> {
            existing.setRsyncId(rsyncId);
            existing.setTime(time);
            existing.setSerialNumber(serialNumber);
            existing.setStatus(CuPmFileSync.Status.CREATED.getValue());
            // Per requirement: update created_at and updated_at on re-ingest
            existing.setCreatedAt(OffsetDateTime.now());
            existing.setUpdatedAt(OffsetDateTime.now());
            cuRepository.save(existing);
            log.debug("Updated existing CU record for path: {}", filePath);
        }, () -> {
            CuPmFileSync record = new CuPmFileSync();
            record.setRsyncId(rsyncId);
            record.setTime(time);
            record.setSerialNumber(serialNumber);
            record.setFilePath(filePath);
            record.setStatus(CuPmFileSync.Status.CREATED.getValue());
            try {
                cuRepository.save(record);
                log.debug("Saved new CU record for path: {}", filePath);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Handle rare race: another thread inserted concurrently
                cuRepository.findByFilePath(filePath).ifPresent(existing -> {
                    existing.setRsyncId(rsyncId);
                    existing.setTime(time);
                    existing.setSerialNumber(serialNumber);
                    existing.setStatus(CuPmFileSync.Status.CREATED.getValue());
                    existing.setCreatedAt(OffsetDateTime.now());
                    existing.setUpdatedAt(OffsetDateTime.now());
                    cuRepository.save(existing);
                    log.debug("Resolved race and updated existing CU record for path: {}", filePath);
                });
            }
        });
    }
    
    private FileMetadata extractFileMetadata(String fileName) {
        Matcher matcher = PM_FILE_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String dateStr = matcher.group(1);        // 20250813
            String startTime = matcher.group(2);     // 2215
            String timezone = matcher.group(3);      // +0530
            String serialNumber = matcher.group(6);  // 001-01-64160
            
            try {
                // Parse date and time
                String dateTimeStr = dateStr + startTime;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
                LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
                
                // Create ZoneOffset from timezone string (+0530 -> +05:30)
                ZoneOffset zoneOffset = ZoneOffset.of(
                    timezone.substring(0, 3) + ":" + timezone.substring(3)
                );
                OffsetDateTime time = localDateTime.atOffset(zoneOffset);
                
                return new FileMetadata(time, serialNumber);
            } catch (Exception e) {
                log.warn("Failed to parse metadata from filename: {}, error: {}", fileName, e.getMessage());
                throw new IllegalArgumentException("Invalid PM file format: " + fileName, e);
            }
        }
        
        throw new IllegalArgumentException("Filename does not match PM file pattern: " + fileName);
    }
    
    // Helper class to hold extracted file metadata
    private static class FileMetadata {
        final OffsetDateTime time;
        final String serialNumber;
        
        FileMetadata(OffsetDateTime time, String serialNumber) {
            this.time = time;
            this.serialNumber = serialNumber;
        }
    }
    
    /**
     * Temporary method to simulate tar file contents for demonstration
     * This simulates the XML files that would be found in the tar archives
     */
    private List<String> simulateTarFileContents(String location) {
        List<String> xmlFiles = new ArrayList<>();
        
        if (location.contains("O-DU")) {
            // Simulate DU PM files
            xmlFiles.add("A20250827.1800+0530-1815+0530_001-01-64160_NRCELL_DU.xml");
            xmlFiles.add("A20250827.1800+0530-1815+0530_001-01-80160_NRCELL_DU.xml");
            xmlFiles.add("A20250827.1800+0530-1815+0530_001-01-96160_NRCELL_DU.xml");
        } else if (location.contains("O-CU")) {
            // Simulate CU PM files
            xmlFiles.add("A20250827.1800+0530-1815+0530_001-01-64160_NRCELL_CU.xml");
            xmlFiles.add("A20250827.1800+0530-1815+0530_001-01-80160_NRCELL_CU.xml");
            xmlFiles.add("A20250827.1800+0530-1815+0530_001-01-96160_NRCELL_CU.xml");
        }
        
        return xmlFiles;
    }
}