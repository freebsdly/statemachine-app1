package com.trina.visiontask.repository;

import com.trina.visiontask.repository.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long>, QuerydslPredicateExecutor<TaskEntity> {
    Optional<TaskEntity> findByTaskId(UUID id);
}
