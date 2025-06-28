package com.emailcampaign.service;

import com.emailcampaign.model.Campaign;
import com.emailcampaign.model.EmailTracking;
import com.emailcampaign.model.Recipient;
import com.emailcampaign.repository.CampaignRepository;
import com.emailcampaign.repository.EmailTrackingRepository;
import com.emailcampaign.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailTrackingRepository emailTrackingRepository;
    private final RecipientRepository recipientRepository;
    private final CampaignRepository campaignRepository;
    private final TrackingService trackingService;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Async
    @Transactional
    public void sendCampaignEmails(Campaign campaign) {
        log.info("Starting to send campaign: {}", campaign.getName());
        
        if (campaign.getRecipientList() == null) {
            log.error("No recipient list found for campaign: {}", campaign.getName());
            return;
        }
        
        List<Recipient> recipients = recipientRepository
                .findByRecipientListAndStatus(campaign.getRecipientList(), 
                        Recipient.RecipientStatus.ACTIVE, Pageable.unpaged())
                .getContent();
        
        int sentCount = 0;
        int failedCount = 0;
        
        for (Recipient recipient : recipients) {
            try {
                sendEmailToRecipient(campaign, recipient);
                sentCount++;
                
                // Log sent event
                logEmailEvent(campaign, recipient, EmailTracking.EventType.SENT, null, null, null);
                
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", recipient.getEmail(), e.getMessage());
                failedCount++;
                
                // Log bounce event
                logEmailEvent(campaign, recipient, EmailTracking.EventType.BOUNCED, null, null, null);
            }
        }
        
        // Update campaign status
        campaign.setStatus(Campaign.CampaignStatus.SENT);
        campaignRepository.save(campaign);
        
        log.info("Campaign {} completed. Sent: {}, Failed: {}", 
                campaign.getName(), sentCount, failedCount);
    }
    
    private void sendEmailToRecipient(Campaign campaign, Recipient recipient) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        // Generate tracking ID
        String trackingId = UUID.randomUUID().toString();
        
        // Prepare email content with tracking pixel
        String emailContent = prepareEmailContent(campaign, recipient, trackingId);
        
        helper.setTo(recipient.getEmail());
        helper.setSubject(campaign.getSubject());
        helper.setFrom(campaign.getSenderEmail(), campaign.getSenderName());
        
        if (campaign.getReplyToEmail() != null && !campaign.getReplyToEmail().isEmpty()) {
            helper.setReplyTo(campaign.getReplyToEmail());
        }
        
        helper.setText(emailContent, true);
        
        mailSender.send(message);
        
        log.debug("Email sent to {} for campaign {}", recipient.getEmail(), campaign.getName());
    }
    
    private String prepareEmailContent(Campaign campaign, Recipient recipient, String trackingId) {
        Context context = new Context();
        context.setVariable("campaign", campaign);
        context.setVariable("recipient", recipient);
        context.setVariable("trackingId", trackingId);
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("unsubscribeUrl", 
                baseUrl + "/api/tracking/unsubscribe?token=" + 
                trackingService.generateUnsubscribeToken(recipient, campaign));
        
        String content = campaign.getContent();
        
        // Add tracking pixel
        String trackingPixel = String.format(
                "<img src=\"%s/api/tracking/pixel/%s\" width=\"1\" height=\"1\" style=\"display:none;\" />",
                baseUrl, trackingId);
        
        content += trackingPixel;
        
        // Process links for click tracking
        content = trackingService.processLinksForTracking(content, campaign, recipient);
        
        return content;
    }
    
    public void sendTestEmail(Campaign campaign, String testEmail) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(testEmail);
        helper.setSubject("[TEST] " + campaign.getSubject());
        helper.setFrom(campaign.getSenderEmail(), campaign.getSenderName());
        helper.setText(campaign.getContent(), true);
        
        mailSender.send(message);
        
        log.info("Test email sent to {} for campaign {}", testEmail, campaign.getName());
    }
    
    private void logEmailEvent(Campaign campaign, Recipient recipient, 
                              EmailTracking.EventType eventType, String ipAddress, 
                              String userAgent, String linkUrl) {
        EmailTracking tracking = new EmailTracking();
        tracking.setTrackingId(UUID.randomUUID().toString());
        tracking.setEventType(eventType);
        tracking.setCampaign(campaign);
        tracking.setRecipient(recipient);
        tracking.setIpAddress(ipAddress);
        tracking.setUserAgent(userAgent);
        tracking.setLinkUrl(linkUrl);
        tracking.setEventTime(LocalDateTime.now());
        
        emailTrackingRepository.save(tracking);
    }
}
