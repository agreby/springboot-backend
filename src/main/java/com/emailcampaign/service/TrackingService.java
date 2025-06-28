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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrackingService {
    
    private final EmailTrackingRepository emailTrackingRepository;
    private final CampaignRepository campaignRepository;
    private final RecipientRepository recipientRepository;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public void trackEmailOpen(String trackingId, HttpServletRequest request) {
        Optional<EmailTracking> existingTracking = emailTrackingRepository.findByTrackingId(trackingId);
        
        if (existingTracking.isPresent()) {
            EmailTracking tracking = existingTracking.get();
            
            // Check if this is the first open
            boolean isFirstOpen = emailTrackingRepository
                    .findByCampaignAndRecipient(tracking.getCampaign(), tracking.getRecipient(), null)
                    .stream()
                    .noneMatch(t -> t.getEventType() == EmailTracking.EventType.OPENED);
            
            if (isFirstOpen) {
                EmailTracking openTracking = new EmailTracking();
                openTracking.setTrackingId(UUID.randomUUID().toString());
                openTracking.setEventType(EmailTracking.EventType.OPENED);
                openTracking.setCampaign(tracking.getCampaign());
                openTracking.setRecipient(tracking.getRecipient());
                openTracking.setIpAddress(getClientIpAddress(request));
                openTracking.setUserAgent(request.getHeader("User-Agent"));
                openTracking.setDeviceType(detectDeviceType(request.getHeader("User-Agent")));
                openTracking.setEmailClient(detectEmailClient(request.getHeader("User-Agent")));
                openTracking.setEventTime(LocalDateTime.now());
                
                emailTrackingRepository.save(openTracking);
                
                log.debug("Email open tracked for campaign: {} recipient: {}", 
                        tracking.getCampaign().getName(), tracking.getRecipient().getEmail());
            }
        }
    }
    
    public void trackLinkClick(String trackingToken, HttpServletRequest request) {
        try {
            String decodedToken = new String(Base64.getDecoder().decode(trackingToken));
            String[] parts = decodedToken.split(":");
            
            if (parts.length >= 4) {
                Long campaignId = Long.parseLong(parts[0]);
                Long recipientId = Long.parseLong(parts[1]);
                String originalUrl = parts[2];
                
                Optional<Campaign> campaign = campaignRepository.findById(campaignId);
                Optional<Recipient> recipient = recipientRepository.findById(recipientId);
                
                if (campaign.isPresent() && recipient.isPresent()) {
                    EmailTracking clickTracking = new EmailTracking();
                    clickTracking.setTrackingId(UUID.randomUUID().toString());
                    clickTracking.setEventType(EmailTracking.EventType.CLICKED);
                    clickTracking.setCampaign(campaign.get());
                    clickTracking.setRecipient(recipient.get());
                    clickTracking.setLinkUrl(originalUrl);
                    clickTracking.setIpAddress(getClientIpAddress(request));
                    clickTracking.setUserAgent(request.getHeader("User-Agent"));
                    clickTracking.setDeviceType(detectDeviceType(request.getHeader("User-Agent")));
                    clickTracking.setEmailClient(detectEmailClient(request.getHeader("User-Agent")));
                    clickTracking.setEventTime(LocalDateTime.now());
                    
                    emailTrackingRepository.save(clickTracking);
                    
                    log.debug("Link click tracked for campaign: {} recipient: {} url: {}", 
                            campaign.get().getName(), recipient.get().getEmail(), originalUrl);
                }
            }
        } catch (Exception e) {
            log.error("Error tracking link click: {}", e.getMessage());
        }
    }
    
    public void trackUnsubscribe(String unsubscribeToken, HttpServletRequest request) {
        try {
            String decodedToken = new String(Base64.getDecoder().decode(unsubscribeToken));
            String[] parts = decodedToken.split(":");
            
            if (parts.length >= 3) {
                Long campaignId = Long.parseLong(parts[0]);
                Long recipientId = Long.parseLong(parts[1]);
                
                Optional<Campaign> campaign = campaignRepository.findById(campaignId);
                Optional<Recipient> recipient = recipientRepository.findById(recipientId);
                
                if (campaign.isPresent() && recipient.isPresent()) {
                    // Update recipient status
                    Recipient r = recipient.get();
                    r.setStatus(Recipient.RecipientStatus.UNSUBSCRIBED);
                    r.setUnsubscribedAt(LocalDateTime.now());
                    recipientRepository.save(r);
                    
                    // Log unsubscribe event
                    EmailTracking unsubscribeTracking = new EmailTracking();
                    unsubscribeTracking.setTrackingId(UUID.randomUUID().toString());
                    unsubscribeTracking.setEventType(EmailTracking.EventType.UNSUBSCRIBED);
                    unsubscribeTracking.setCampaign(campaign.get());
                    unsubscribeTracking.setRecipient(r);
                    unsubscribeTracking.setIpAddress(getClientIpAddress(request));
                    unsubscribeTracking.setUserAgent(request.getHeader("User-Agent"));
                    unsubscribeTracking.setEventTime(LocalDateTime.now());
                    
                    emailTrackingRepository.save(unsubscribeTracking);
                    
                    log.info("Unsubscribe tracked for campaign: {} recipient: {}", 
                            campaign.get().getName(), r.getEmail());
                }
            }
        } catch (Exception e) {
            log.error("Error tracking unsubscribe: {}", e.getMessage());
        }
    }
    
    public String processLinksForTracking(String content, Campaign campaign, Recipient recipient) {
        Pattern linkPattern = Pattern.compile("<a\\s+href=\"([^\"]+)\"([^>]*)>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = linkPattern.matcher(content);
        
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            String otherAttributes = matcher.group(2);
            
            // Skip tracking pixel and unsubscribe links
            if (!originalUrl.contains("/api/tracking/")) {
                String trackingToken = generateClickTrackingToken(campaign, recipient, originalUrl);
                String trackedUrl = baseUrl + "/api/tracking/click/" + trackingToken;
                
                String replacement = "<a href=\"" + trackedUrl + "\"" + otherAttributes + ">";
                matcher.appendReplacement(result, replacement);
            }
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    public String generateClickTrackingToken(Campaign campaign, Recipient recipient, String originalUrl) {
        String tokenData = campaign.getId() + ":" + recipient.getId() + ":" + originalUrl + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(tokenData.getBytes());
    }
    
    public String generateUnsubscribeToken(Recipient recipient, Campaign campaign) {
        String tokenData = campaign.getId() + ":" + recipient.getId() + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(tokenData.getBytes());
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String detectDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        userAgent = userAgent.toLowerCase();
        
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
    
    private String detectEmailClient(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        userAgent = userAgent.toLowerCase();
        
        if (userAgent.contains("outlook")) return "Outlook";
        if (userAgent.contains("thunderbird")) return "Thunderbird";
        if (userAgent.contains("apple mail")) return "Apple Mail";
        if (userAgent.contains("gmail")) return "Gmail";
        if (userAgent.contains("yahoo")) return "Yahoo Mail";
        
        return "Unknown";
    }
}
