package com.emailcampaign.dto;

import com.emailcampaign.model.Campaign;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CampaignDto {
    
    private Long id;
    
    @NotBlank(message = "Campaign name is required")
    private String name;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String senderName;
    private String senderEmail;
    private String replyToEmail;
    private String content;
    private Campaign.CampaignType type;
    private Campaign.CampaignStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long recipientListId;
    private String recipientListName;
    private Long userId;
    private String username;
    private String recipientEmail;
}
