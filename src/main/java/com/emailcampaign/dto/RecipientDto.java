package com.emailcampaign.dto;

import com.emailcampaign.model.Recipient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecipientDto {
    
    private Long id;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    private String firstName;
    private String lastName;
    private Recipient.RecipientStatus status;
    private LocalDateTime subscribedAt;
    private LocalDateTime unsubscribedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long recipientListId;
    private String recipientListName;
}
