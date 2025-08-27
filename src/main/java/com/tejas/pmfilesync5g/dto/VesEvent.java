package com.tejas.pmfilesync5g.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VesEvent {
    
    @JsonProperty("event")
    private Event event;

    @Data
    public static class Event {
        @JsonProperty("commonEventHeader")
        private CommonEventHeader commonEventHeader;
        
        @JsonProperty("notificationFields")
        private NotificationFields notificationFields;
    }

    @Data
    public static class CommonEventHeader {
        @JsonProperty("domain")
        private String domain;
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("eventId")
        private String eventId;
        
        @JsonProperty("eventName")
        private String eventName;
        
        @JsonProperty("sourceName")
        private String sourceName;
        
        @JsonProperty("reportingEntityName")
        private String reportingEntityName;
    }

    @Data
    public static class NotificationFields {
        @JsonProperty("changeIdentifier")
        private String changeIdentifier;
        
        @JsonProperty("changeType")
        private String changeType;
        
        @JsonProperty("notificationFieldsVersion")
        private String notificationFieldsVersion;
        
        @JsonProperty("arrayOfNamedHashMap")
        private List<NamedHashMap> arrayOfNamedHashMap;
    }

    @Data
    public static class NamedHashMap {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("hashMap")
        private HashMapContent hashMap;
    }

    @Data
    public static class HashMapContent {
        @JsonProperty("location")
        private String location;
        
        @JsonProperty("compression")
        private String compression;
        
        @JsonProperty("fileFormatType")
        private String fileFormatType;
        
        @JsonProperty("fileSize")
        private String fileSize;
        
        @JsonProperty("fileDataType")
        private String fileDataType;
        
        @JsonProperty("fileFormatVersion")
        private String fileFormatVersion;
        
        @JsonProperty("md5Checksum")
        private String md5Checksum;
    }
}