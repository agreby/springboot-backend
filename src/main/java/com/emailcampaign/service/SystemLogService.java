package com.emailcampaign.service;

import com.emailcampaign.model.SystemLog;
import com.emailcampaign.model.User;
import com.emailcampaign.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SystemLogService {
    
    private final SystemLogRepository systemLogRepository;
    
    public void logUserAction(User user, String action, String description, 
                             String ipAddress, String userAgent) {
        SystemLog systemLog = new SystemLog();
        systemLog.setUser(user);
        systemLog.setLevel(SystemLog.LogLevel.INFO);
        systemLog.setAction(action);
        systemLog.setDescription(description);
        systemLog.setIpAddress(ipAddress);
        systemLog.setUserAgent(userAgent);
        systemLog.setCreatedAt(LocalDateTime.now());
        
        systemLogRepository.save(systemLog);
        
        log.info("User action logged: {} - {} - {}", 
                user != null ? user.getUsername() : "System", action, description);
    }
    
    public void logSystemEvent(SystemLog.LogLevel level, String action, String description) {
        SystemLog systemLog = new SystemLog();
        systemLog.setLevel(level);
        systemLog.setAction(action);
        systemLog.setDescription(description);
        systemLog.setCreatedAt(LocalDateTime.now());
        
        systemLogRepository.save(systemLog);
        
        log.info("System event logged: {} - {} - {}", level, action, description);
    }
    
    public Page<SystemLog> getSystemLogs(Pageable pageable) {
        return systemLogRepository.findAll(pageable);
    }
    
    public Page<SystemLog> getSystemLogsByUser(User user, Pageable pageable) {
        return systemLogRepository.findByUser(user, pageable);
    }
    
    public Page<SystemLog> getSystemLogsByLevel(SystemLog.LogLevel level, Pageable pageable) {
        return systemLogRepository.findByLevel(level, pageable);
    }
    
    public Page<SystemLog> getSystemLogsByDateRange(LocalDateTime startDate, 
                                                   LocalDateTime endDate, 
                                                   Pageable pageable) {
        return systemLogRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }
    
    public Page<SystemLog> searchSystemLogs(String action, Pageable pageable) {
        return systemLogRepository.findByActionContaining(action, pageable);
    }
}
