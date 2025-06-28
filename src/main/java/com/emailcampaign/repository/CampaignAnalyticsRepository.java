package com.emailcampaign.repository;

import com.emailcampaign.model.Campaign;
import com.emailcampaign.model.CampaignAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignAnalyticsRepository extends JpaRepository<CampaignAnalytics, Long> {
    
    Optional<CampaignAnalytics> findByCampaign(Campaign campaign);
    
    void deleteByCampaign(Campaign campaign);
}
