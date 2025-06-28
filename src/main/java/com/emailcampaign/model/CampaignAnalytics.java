package com.emailcampaign.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "total_sent", nullable = false)
    private Integer totalSent = 0;
    
    @Column(name = "total_delivered", nullable = false)
    private Integer totalDelivered = 0;
    
    @Column(name = "total_opened", nullable = false)
    private Integer totalOpened = 0;
    
    @Column(name = "total_clicked", nullable = false)
    private Integer totalClicked = 0;
    
    @Column(name = "total_bounced", nullable = false)
    private Integer totalBounced = 0;
    
    @Column(name = "total_unsubscribed", nullable = false)
    private Integer totalUnsubscribed = 0;
    
    @Column(name = "open_rate", nullable = false)
    private Double openRate = 0.0;
    
    @Column(name = "click_rate", nullable = false)
    private Double clickRate = 0.0;
    
    @Column(name = "bounce_rate", nullable = false)
    private Double bounceRate = 0.0;
    
    @Column(name = "unsubscribe_rate", nullable = false)
    private Double unsubscribeRate = 0.0;
    
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
}
