package com.emailcampaign.repository;

import com.emailcampaign.model.SystemLog;
import com.emailcampaign.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    
    Page<SystemLog> findByUser(User user, Pageable pageable);
    
    Page<SystemLog> findByLevel(SystemLog.LogLevel level, Pageable pageable);
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.createdAt BETWEEN :startDate AND :endDate")
    Page<SystemLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.action LIKE %:action%")
    Page<SystemLog> findByActionContaining(String action, Pageable pageable);
}
