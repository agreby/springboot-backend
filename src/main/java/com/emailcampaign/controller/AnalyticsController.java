package com.emailcampaign.controller;

import com.emailcampaign.dto.ApiResponse;
import com.emailcampaign.dto.CampaignAnalyticsDto;
import com.emailcampaign.model.Campaign;
import com.emailcampaign.model.User;
import com.emailcampaign.service.AnalyticsService;
import com.emailcampaign.service.CampaignService;
import com.emailcampaign.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final CampaignService campaignService;
    private final UserService userService;
    
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<ApiResponse> getCampaignAnalytics(@PathVariable Long campaignId,
                                                           Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Optional<Campaign> campaign = campaignService.findById(campaignId);
            
            if (campaign.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Campaign not found"));
            }
            
            // Check access
            if (!campaign.get().getUser().getId().equals(user.getId()) && 
                user.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied"));
            }
            
            CampaignAnalyticsDto analytics = analyticsService.getCampaignAnalytics(campaign.get());
            return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
            
        } catch (Exception e) {
            log.error("Error retrieving campaign analytics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving analytics"));
        }
    }
    
    @PostMapping("/campaigns/{campaignId}/calculate")
    public ResponseEntity<ApiResponse> calculateCampaignAnalytics(@PathVariable Long campaignId,
                                                                 Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Optional<Campaign> campaign = campaignService.findById(campaignId);
            
            if (campaign.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Campaign not found"));
            }
            
            // Check access
            if (!campaign.get().getUser().getId().equals(user.getId()) && 
                user.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied"));
            }
            
            analyticsService.calculateCampaignAnalytics(campaign.get());
            return ResponseEntity.ok(ApiResponse.success("Analytics calculated successfully"));
            
        } catch (Exception e) {
            log.error("Error calculating campaign analytics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error calculating analytics"));
        }
    }
    
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
