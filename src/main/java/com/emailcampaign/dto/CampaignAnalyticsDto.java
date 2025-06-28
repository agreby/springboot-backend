package com.emailcampaign.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CampaignAnalyticsDto {
    
    private Long campaignId;
    private String campaignName;
    private String subject;
    private Integer totalSent;
    private Integer totalDelivered;
    private Integer totalOpened;
    private Integer totalClicked;
    private Integer totalBounced;
    private Integer totalUnsubscribed;
    private Double openRate;
    private Double clickRate;
    private Double bounceRate;
    private Double unsubscribeRate;
    private LocalDateTime calculatedAt;
    private List<HourlyStatsDto> hourlyStats;
    private List<DeviceStatsDto> deviceStats;
    private List<LocationStatsDto> locationStats;
    
    @Data
    public static class HourlyStatsDto {
        private Integer hour;
        private Integer opens;
        private Integer clicks;
    }
    
    @Data
    public static class DeviceStatsDto {
        private String deviceType;
        private Integer count;
        private Double percentage;
    }
    
    @Data
    public static class LocationStatsDto {
        private String location;
        private Integer count;
        private Double percentage;
    }
}
