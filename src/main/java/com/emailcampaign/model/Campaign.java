package com.emailcampaign.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "campaigns")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Campaign name is required")
    @Column(nullable = false)
    private String name;
    
    @NotBlank(message = "Subject is required")
    @Column(nullable = false)
    private String subject;
    
    @Column(name = "sender_name", nullable = false)
    private String senderName;
    
    @Column(name = "sender_email", nullable = false)
    private String senderEmail;
    
    @Column(name = "reply_to_email")
    private String replyToEmail;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignType type = CampaignType.REGULAR;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_list_id")
    private RecipientList recipientList;
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmailTracking> emailTrackings;
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CampaignAnalytics> analytics;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum CampaignType {
        REGULAR, AB_TEST, AUTOMATED, RSS
    }
    
    public enum CampaignStatus {
        DRAFT, SCHEDULED, SENDING, SENT, PAUSED, CANCELLED
    }
}
