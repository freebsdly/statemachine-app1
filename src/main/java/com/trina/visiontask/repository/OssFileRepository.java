package com.trina.visiontask.repository;

import com.trina.visiontask.repository.entity.OssFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OssFileRepository extends JpaRepository<OssFileEntity, String>,
        QuerydslPredicateExecutor<OssFileEntity> {

    Optional<OssFileEntity> findByOriginalId(String id);

    Optional<OssFileEntity> findByUrl(String id);

    void deleteByOriginalId(String id);
}