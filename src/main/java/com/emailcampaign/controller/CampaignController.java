package com.emailcampaign.controller;

import com.emailcampaign.dto.ApiResponse;
import com.emailcampaign.dto.CampaignDto;
import com.emailcampaign.model.Campaign;
import com.emailcampaign.model.User;
import com.emailcampaign.service.CampaignService;
import com.emailcampaign.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class CampaignController {
    
    private final CampaignService campaignService;
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<ApiResponse> createCampaign(@Valid @RequestBody CampaignDto campaignDto,
                                                    Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Campaign campaign = campaignService.createCampaign(campaignDto, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Campaign created successfully", campaign));
        } catch (Exception e) {
            log.error("Error creating campaign: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse> getUserCampaigns(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(defaultValue = "createdAt") String sortBy,
                                                       @RequestParam(defaultValue = "desc") String sortDir,
                                                       Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Campaign> campaigns = campaignService.getUserCampaigns(user, pageable);
            return ResponseEntity.ok(ApiResponse.success("Campaigns retrieved successfully", campaigns));
        } catch (Exception e) {
            log.error("Error retrieving campaigns: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving campaigns"));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getCampaign(@PathVariable Long id,
                                                 Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Optional<Campaign> campaign = campaignService.findById(id);
            
            if (campaign.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Campaign not found"));
            }
            
            // Check if user owns the campaign or is admin
            if (!campaign.get().getUser().getId().equals(user.getId()) && 
                user.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Campaign retrieved successfully", campaign.get()));
        } catch (Exception e) {
            log.error("Error retrieving campaign: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving campaign"));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateCampaign(@PathVariable Long id,
                                                    @Valid @RequestBody CampaignDto campaignDto,
                                                    Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Campaign campaign = campaignService.updateCampaign(id, campaignDto, user);
            return ResponseEntity.ok(ApiResponse.success("Campaign updated successfully", campaign));
        } catch (Exception e) {
            log.error("Error updating campaign: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse> sendCampaign(@PathVariable Long id,
                                                  Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            campaignService.sendCampaign(id, user);
            return ResponseEntity.ok(ApiResponse.success("Campaign sent successfully"));
        } catch (Exception e) {
            log.error("Error sending campaign: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse> scheduleCampaign(@PathVariable Long id,
                                                      @RequestParam String scheduledAt,
                                                      Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            LocalDateTime scheduleTime = LocalDateTime.parse(scheduledAt);
            campaignService.scheduleCampaign(id, scheduleTime, user);
            return ResponseEntity.ok(ApiResponse.success("Campaign scheduled successfully"));
        } catch (Exception e) {
            log.error("Error scheduling campaign: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCampaign(@PathVariable Long id,
                                                    Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            campaignService.deleteCampaign(id, user);
            return ResponseEntity.ok(ApiResponse.success("Campaign deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting campaign: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchCampaigns(@RequestParam String name,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<Campaign> campaigns = campaignService.searchUserCampaigns(user, name, pageable);
            return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", campaigns));
        } catch (Exception e) {
            log.error("Error searching campaigns: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error searching campaigns"));
        }
    }
    
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
