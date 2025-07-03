package com.emailcampaign.service;

import com.emailcampaign.dto.CampaignDto;
import com.emailcampaign.model.Campaign;
import com.emailcampaign.model.RecipientList;
import com.emailcampaign.model.User;
import com.emailcampaign.model.Recipient;
import com.emailcampaign.repository.CampaignRepository;
import com.emailcampaign.repository.RecipientListRepository;
import com.emailcampaign.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CampaignService {
    
    private final CampaignRepository campaignRepository;
    private final RecipientListRepository recipientListRepository;
    private final RecipientRepository recipientRepository;
    private final EmailService emailService;
    private final SystemLogService systemLogService;
    
    public Campaign createCampaign(CampaignDto campaignDto, User user) {
        Campaign campaign = new Campaign();
        campaign.setName(campaignDto.getName());
        campaign.setSubject(campaignDto.getSubject());
        campaign.setSenderName(campaignDto.getSenderName());
        campaign.setSenderEmail(campaignDto.getSenderEmail());
        campaign.setReplyToEmail(campaignDto.getReplyToEmail());
        campaign.setContent(campaignDto.getContent());
        campaign.setType(campaignDto.getType() != null ? campaignDto.getType() : Campaign.CampaignType.REGULAR);
        campaign.setStatus(Campaign.CampaignStatus.DRAFT);
        campaign.setUser(user);
        
        if (campaignDto.getRecipientEmail() != null && !campaignDto.getRecipientEmail().isEmpty()) {
            // Create a temporary recipient list
            RecipientList tempList = new RecipientList();
            tempList.setName("Single Send " + campaignDto.getRecipientEmail() + " [" + System.currentTimeMillis() + "]");
            tempList.setDescription("Temporary list for single send");
            tempList.setUser(user);
            tempList = recipientListRepository.save(tempList);

            // Create the recipient
            Recipient recipient = new Recipient();
            recipient.setEmail(campaignDto.getRecipientEmail());
            recipient.setStatus(Recipient.RecipientStatus.ACTIVE);
            recipient.setRecipientList(tempList);
            recipientRepository.save(recipient);

            campaign.setRecipientList(tempList);
        } else if (campaignDto.getRecipientListId() != null) {
            RecipientList recipientList = recipientListRepository.findById(campaignDto.getRecipientListId())
                    .orElseThrow(() -> new RuntimeException("Recipient list not found"));
            campaign.setRecipientList(recipientList);
        }
        
        if (campaignDto.getScheduledAt() != null) {
            campaign.setScheduledAt(campaignDto.getScheduledAt());
            campaign.setStatus(Campaign.CampaignStatus.SCHEDULED);
        }
        
        Campaign savedCampaign = campaignRepository.save(campaign);
        
        systemLogService.logUserAction(user, "CAMPAIGN_CREATED", 
                "Campaign created: " + savedCampaign.getName(), null, null);
        
        log.info("Campaign created: {} by user: {}", savedCampaign.getName(), user.getUsername());
        return savedCampaign;
    }
    
    public Campaign updateCampaign(Long id, CampaignDto campaignDto, User user) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (!campaign.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        if (campaign.getStatus() == Campaign.CampaignStatus.SENT) {
            throw new RuntimeException("Cannot update sent campaign");
        }
        
        campaign.setName(campaignDto.getName());
        campaign.setSubject(campaignDto.getSubject());
        campaign.setSenderName(campaignDto.getSenderName());
        campaign.setSenderEmail(campaignDto.getSenderEmail());
        campaign.setReplyToEmail(campaignDto.getReplyToEmail());
        campaign.setContent(campaignDto.getContent());
        
        if (campaignDto.getRecipientListId() != null) {
            RecipientList recipientList = recipientListRepository.findById(campaignDto.getRecipientListId())
                    .orElseThrow(() -> new RuntimeException("Recipient list not found"));
            campaign.setRecipientList(recipientList);
        }
        
        Campaign savedCampaign = campaignRepository.save(campaign);
        
        systemLogService.logUserAction(user, "CAMPAIGN_UPDATED", 
                "Campaign updated: " + savedCampaign.getName(), null, null);
        
        return savedCampaign;
    }
    
    public void sendCampaign(Long id, User user) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (!campaign.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        if (campaign.getStatus() != Campaign.CampaignStatus.DRAFT && 
            campaign.getStatus() != Campaign.CampaignStatus.SCHEDULED) {
            throw new RuntimeException("Campaign cannot be sent in current status");
        }
        
        campaign.setStatus(Campaign.CampaignStatus.SENDING);
        campaign.setSentAt(LocalDateTime.now());
        campaignRepository.save(campaign);
        
        // Send emails asynchronously
        emailService.sendCampaignEmails(campaign);
        
        systemLogService.logUserAction(user, "CAMPAIGN_SENT", 
                "Campaign sent: " + campaign.getName(), null, null);
        
        log.info("Campaign sent: {} by user: {}", campaign.getName(), user.getUsername());
    }
    
    public void scheduleCampaign(Long id, LocalDateTime scheduledAt, User user) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (!campaign.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot schedule campaign in the past");
        }
        
        campaign.setScheduledAt(scheduledAt);
        campaign.setStatus(Campaign.CampaignStatus.SCHEDULED);
        campaignRepository.save(campaign);
        
        systemLogService.logUserAction(user, "CAMPAIGN_SCHEDULED", 
                "Campaign scheduled: " + campaign.getName() + " for " + scheduledAt, null, null);
    }
    
    public void deleteCampaign(Long id, User user) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (!campaign.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        
        if (campaign.getStatus() == Campaign.CampaignStatus.SENDING) {
            throw new RuntimeException("Cannot delete campaign that is currently being sent");
        }
        
        campaignRepository.delete(campaign);
        
        systemLogService.logUserAction(user, "CAMPAIGN_DELETED", 
                "Campaign deleted: " + campaign.getName(), null, null);
    }
    
    public Optional<Campaign> findById(Long id) {
        return campaignRepository.findById(id);
    }
    
    public Page<Campaign> getUserCampaigns(User user, Pageable pageable) {
        return campaignRepository.findByUser(user, pageable);
    }
    
    public Page<Campaign> getUserCampaignsByStatus(User user, Campaign.CampaignStatus status, Pageable pageable) {
        return campaignRepository.findByUserAndStatus(user, status, pageable);
    }
    
    public Page<Campaign> searchUserCampaigns(User user, String name, Pageable pageable) {
        return campaignRepository.findByUserAndNameContaining(user, name, pageable);
    }
    
    public List<Campaign> getScheduledCampaigns() {
        return campaignRepository.findByStatusAndScheduledAtBefore(
                Campaign.CampaignStatus.SCHEDULED, LocalDateTime.now());
    }
    
    public long getUserCampaignCount(User user, Campaign.CampaignStatus status) {
        return campaignRepository.countByUserAndStatus(user, status);
    }
    
    public Page<Campaign> getAllCampaigns(Pageable pageable) {
        return campaignRepository.findAll(pageable);
    }
}
