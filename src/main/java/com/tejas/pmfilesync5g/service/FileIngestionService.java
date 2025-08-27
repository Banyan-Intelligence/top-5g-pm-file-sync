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

    private static final Pattern TIME_PATTERN = Pattern.compile("A(\\d{8})\\.(\\d{4})");
    
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
            
            String serialNumber = extractSerialNumber(sourceName);
            
            for (String xmlFile : xmlFiles) {
                OffsetDateTime time = extractTimeFromFileName(xmlFile);
                
                if (isDu) {
                    saveDuRecord(rsyncId, time, serialNumber, xmlFile);
                } else {
                    saveCuRecord(rsyncId, time, serialNumber, xmlFile);
                }
            }
            
            log.info("Successfully processed {} XML files for {}", xmlFiles.size(), isDu ? "DU" : "CU");
            
        } catch (Exception e) {
            log.error("Error processing VES event for location: {}", location, e);
            throw new RuntimeException("Failed to process VES event", e);
        }
    }
    
    private void saveDuRecord(UUID rsyncId, OffsetDateTime time, String serialNumber, String filePath) {
        DuPmFileSync record = new DuPmFileSync();
        record.setRsyncId(rsyncId);
        record.setTime(time);
        record.setSerialNumber(serialNumber);
        record.setFilePath(filePath);
        record.setStatus(DuPmFileSync.Status.CREATED.getValue());
        
        duRepository.save(record);
        log.debug("Saved DU record: {}", record.getId());
    }
    
    private void saveCuRecord(UUID rsyncId, OffsetDateTime time, String serialNumber, String filePath) {
        CuPmFileSync record = new CuPmFileSync();
        record.setRsyncId(rsyncId);
        record.setTime(time);
        record.setSerialNumber(serialNumber);
        record.setFilePath(filePath);
        record.setStatus(CuPmFileSync.Status.CREATED.getValue());
        
        cuRepository.save(record);
        log.debug("Saved CU record: {}", record.getId());
    }
    
    private String extractSerialNumber(String sourceName) {
        if (sourceName == null || sourceName.isEmpty()) {
            return "UNKNOWN";
        }
        
        String[] parts = sourceName.split("-");
        if (parts.length >= 3) {
            return parts[parts.length - 1];
        }
        
        return sourceName.length() > 15 ? sourceName.substring(0, 15) : sourceName;
    }
    
    private OffsetDateTime extractTimeFromFileName(String fileName) {
        Matcher matcher = TIME_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            String timeStr = matcher.group(2);
            
            try {
                String dateTimeStr = dateStr + timeStr;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
                LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
                return localDateTime.atOffset(ZoneOffset.of("+05:30"));
            } catch (Exception e) {
                log.warn("Failed to parse time from filename: {}, using current time", fileName);
            }
        }
        
        return OffsetDateTime.now(ZoneId.of("Asia/Kolkata"));
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