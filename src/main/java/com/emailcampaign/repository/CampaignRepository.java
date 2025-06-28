package com.emailcampaign.repository;

import com.emailcampaign.model.Campaign;
import com.emailcampaign.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    
    Page<Campaign> findByUser(User user, Pageable pageable);
    
    Page<Campaign> findByUserAndStatus(User user, Campaign.CampaignStatus status, Pageable pageable);
    
    List<Campaign> findByStatusAndScheduledAtBefore(Campaign.CampaignStatus status, LocalDateTime dateTime);
    
    @Query("SELECT c FROM Campaign c WHERE c.user = :user AND c.name LIKE %:name%")
    Page<Campaign> findByUserAndNameContaining(User user, String name, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.user = :user AND c.status = :status")
    long countByUserAndStatus(User user, Campaign.CampaignStatus status);
    
    @Query("SELECT c FROM Campaign c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<Campaign> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
