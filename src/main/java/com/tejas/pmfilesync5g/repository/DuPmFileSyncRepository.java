package com.tejas.pmfilesync5g.repository;

import com.tejas.pmfilesync5g.entity.DuPmFileSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface DuPmFileSyncRepository extends JpaRepository<DuPmFileSync, UUID> {
    Optional<DuPmFileSync> findByFilePath(String filePath);
}