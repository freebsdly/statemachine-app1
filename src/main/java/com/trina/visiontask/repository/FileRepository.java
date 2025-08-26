package com.trina.visiontask.repository;

import com.trina.visiontask.repository.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long>, QuerydslPredicateExecutor<FileEntity> {
    Optional<FileEntity> findByFileId(UUID id);

    Optional<FileEntity> findByFileName(String fileName);
}
