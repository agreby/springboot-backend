package com.emailcampaign.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tracking_id", unique = true, nullable = false)
    private String trackingId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;
    
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime = LocalDateTime.now();
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "device_type")
    private String deviceType;
    
    @Column(name = "email_client")
    private String emailClient;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "link_url")
    private String linkUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;
    
    public enum EventType {
        SENT, DELIVERED, OPENED, CLICKED, BOUNCED, COMPLAINED, UNSUBSCRIBED
    }
}
