package com.tejas.pmfilesync5g.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cu_pm_file_sync")
@Data
public class CuPmFileSync {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "rsync_id", nullable = false)
    private UUID rsyncId;

    @Column(name = "time", nullable = false)
    private OffsetDateTime time;

    @Column(name = "serial_number", nullable = false, length = 15)
    private String serialNumber;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "status")
    private Short status;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum Status {
        CREATED((short) 0),
        IN_PROGRESS((short) 1),
        COMPLETED((short) 2);

        private final short value;

        Status(short value) {
            this.value = value;
        }

        public short getValue() {
            return value;
        }
    }
}