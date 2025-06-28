package com.emailcampaign.service;

import com.emailcampaign.dto.CampaignAnalyticsDto;
import com.emailcampaign.model.Campaign;
import com.emailcampaign.model.CampaignAnalytics;
import com.emailcampaign.model.EmailTracking;
import com.emailcampaign.repository.CampaignAnalyticsRepository;
import com.emailcampaign.repository.EmailTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AnalyticsService {
    
    private final EmailTrackingRepository emailTrackingRepository;
    private final CampaignAnalyticsRepository campaignAnalyticsRepository;
    
    public CampaignAnalyticsDto getCampaignAnalytics(Campaign campaign) {
        CampaignAnalytics analytics = campaignAnalyticsRepository.findByCampaign(campaign)
                .orElseGet(() -> calculateCampaignAnalytics(campaign));
        
        CampaignAnalyticsDto dto = new CampaignAnalyticsDto();
        dto.setCampaignId(campaign.getId());
        dto.setCampaignName(campaign.getName());
        dto.setSubject(campaign.getSubject());
        dto.setTotalSent(analytics.getTotalSent());
        dto.setTotalDelivered(analytics.getTotalDelivered());
        dto.setTotalOpened(analytics.getTotalOpened());
        dto.setTotalClicked(analytics.getTotalClicked());
        dto.setTotalBounced(analytics.getTotalBounced());
        dto.setTotalUnsubscribed(analytics.getTotalUnsubscribed());
        dto.setOpenRate(analytics.getOpenRate());
        dto.setClickRate(analytics.getClickRate());
        dto.setBounceRate(analytics.getBounceRate());
        dto.setUnsubscribeRate(analytics.getUnsubscribeRate());
        dto.setCalculatedAt(analytics.getCalculatedAt());
        
        // Add detailed analytics
        dto.setHourlyStats(getHourlyStats(campaign));
        dto.setDeviceStats(getDeviceStats(campaign));
        dto.setLocationStats(getLocationStats(campaign));
        
        return dto;
    }
    
    public CampaignAnalytics calculateCampaignAnalytics(Campaign campaign) {
        CampaignAnalytics analytics = campaignAnalyticsRepository.findByCampaign(campaign)
                .orElse(new CampaignAnalytics());
        
        analytics.setCampaign(campaign);
        
        // Calculate totals
        analytics.setTotalSent((int) emailTrackingRepository
                .countByCampaignAndEventType(campaign, EmailTracking.EventType.SENT));
        
        analytics.setTotalDelivered((int) emailTrackingRepository
                .countByCampaignAndEventType(campaign, EmailTracking.EventType.DELIVERED));
        
        analytics.setTotalOpened((int) emailTrackingRepository
                .countByCampaignAndEventType(campaign, EmailTracking.EventType.OPENED));
        
        analytics.setTotalClicked((int) emailTrackingRepository
                .countByCampaignAndEventType(campaign, EmailTracking.EventType.CLICKED));
        
        analytics.setTotalBounced((int) emailTrackingRepository
                .countByCampaignAndEventType(campaign, EmailTracking.EventType.BOUNCED));
        
        analytics.setTotalUnsubscribed((int) emailTrackingRepository
                .countByCampaignAndEventType(campaign, EmailTracking.EventType.UNSUBSCRIBED));
        
        // Calculate rates
        if (analytics.getTotalSent() > 0) {
            analytics.setOpenRate((double) analytics.getTotalOpened() / analytics.getTotalSent() * 100);
            analytics.setClickRate((double) analytics.getTotalClicked() / analytics.getTotalSent() * 100);
            analytics.setBounceRate((double) analytics.getTotalBounced() / analytics.getTotalSent() * 100);
            analytics.setUnsubscribeRate((double) analytics.getTotalUnsubscribed() / analytics.getTotalSent() * 100);
        }
        
        analytics.setCalculatedAt(LocalDateTime.now());
        
        return campaignAnalyticsRepository.save(analytics);
    }
    
    private List<CampaignAnalyticsDto.HourlyStatsDto> getHourlyStats(Campaign campaign) {
        List<EmailTracking> trackings = emailTrackingRepository.findByCampaign(campaign);
        
        Map<Integer, Long> opensByHour = trackings.stream()
                .filter(t -> t.getEventType() == EmailTracking.EventType.OPENED)
                .collect(Collectors.groupingBy(
                        t -> t.getEventTime().getHour(),
                        Collectors.counting()
                ));
        
        Map<Integer, Long> clicksByHour = trackings.stream()
                .filter(t -> t.getEventType() == EmailTracking.EventType.CLICKED)
                .collect(Collectors.groupingBy(
                        t -> t.getEventTime().getHour(),
                        Collectors.counting()
                ));
        
        return opensByHour.entrySet().stream()
                .map(entry -> {
                    CampaignAnalyticsDto.HourlyStatsDto dto = new CampaignAnalyticsDto.HourlyStatsDto();
                    dto.setHour(entry.getKey());
                    dto.setOpens(entry.getValue().intValue());
                    dto.setClicks(clicksByHour.getOrDefault(entry.getKey(), 0L).intValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    private List<CampaignAnalyticsDto.DeviceStatsDto> getDeviceStats(Campaign campaign) {
        List<EmailTracking> trackings = emailTrackingRepository.findByCampaign(campaign);
        
        Map<String, Long> deviceCounts = trackings.stream()
                .filter(t -> t.getDeviceType() != null)
                .collect(Collectors.groupingBy(
                        EmailTracking::getDeviceType,
                        Collectors.counting()
                ));
        
        long total = deviceCounts.values().stream().mapToLong(Long::longValue).sum();
        
        return deviceCounts.entrySet().stream()
                .map(entry -> {
                    CampaignAnalyticsDto.DeviceStatsDto dto = new CampaignAnalyticsDto.DeviceStatsDto();
                    dto.setDeviceType(entry.getKey());
                    dto.setCount(entry.getValue().intValue());
                    dto.setPercentage(total > 0 ? (double) entry.getValue() / total * 100 : 0.0);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    private List<CampaignAnalyticsDto.LocationStatsDto> getLocationStats(Campaign campaign) {
        List<EmailTracking> trackings = emailTrackingRepository.findByCampaign(campaign);
        
        Map<String, Long> locationCounts = trackings.stream()
                .filter(t -> t.getLocation() != null)
                .collect(Collectors.groupingBy(
                        EmailTracking::getLocation,
                        Collectors.counting()
                ));
        
        long total = locationCounts.values().stream().mapToLong(Long::longValue).sum();
        
        return locationCounts.entrySet().stream()
                .map(entry -> {
                    CampaignAnalyticsDto.LocationStatsDto dto = new CampaignAnalyticsDto.LocationStatsDto();
                    dto.setLocation(entry.getKey());
                    dto.setCount(entry.getValue().intValue());
                    dto.setPercentage(total > 0 ? (double) entry.getValue() / total * 100 : 0.0);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void updateCampaignAnalytics() {
        log.debug("Running scheduled analytics update");
        
        // This would typically update analytics for recently active campaigns
        // Implementation depends on your specific requirements
    }
}
