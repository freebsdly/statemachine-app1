package com.trina.visiontask.repository;

import com.trina.visiontask.repository.entity.TaskHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntity, Long>, QuerydslPredicateExecutor<TaskHistoryEntity> {
}