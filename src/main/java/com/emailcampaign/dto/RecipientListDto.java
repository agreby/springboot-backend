package com.emailcampaign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecipientListDto {
    
    private Long id;
    
    @NotBlank(message = "List name is required")
    private String name;
    
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String username;
    private Long recipientCount;
}
