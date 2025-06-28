package com.emailcampaign.repository;

import com.emailcampaign.model.Campaign;
import com.emailcampaign.model.EmailTracking;
import com.emailcampaign.model.Recipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTrackingRepository extends JpaRepository<EmailTracking, Long> {
    
    Optional<EmailTracking> findByTrackingId(String trackingId);
    
    List<EmailTracking> findByCampaign(Campaign campaign);
    
    List<EmailTracking> findByCampaignAndEventType(Campaign campaign, EmailTracking.EventType eventType);
    
    Page<EmailTracking> findByCampaignAndRecipient(Campaign campaign, Recipient recipient, Pageable pageable);
    
    @Query("SELECT COUNT(et) FROM EmailTracking et WHERE et.campaign = :campaign AND et.eventType = :eventType")
    long countByCampaignAndEventType(Campaign campaign, EmailTracking.EventType eventType);
    
    @Query("SELECT et FROM EmailTracking et WHERE et.campaign = :campaign AND et.eventTime BETWEEN :startDate AND :endDate")
    List<EmailTracking> findByCampaignAndEventTimeBetween(Campaign campaign, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT DISTINCT et.recipient FROM EmailTracking et WHERE et.campaign = :campaign AND et.eventType = :eventType")
    List<Recipient> findDistinctRecipientsByCampaignAndEventType(Campaign campaign, EmailTracking.EventType eventType);
}
