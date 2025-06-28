package com.emailcampaign.controller;

import com.emailcampaign.dto.ApiResponse;
import com.emailcampaign.dto.RecipientDto;
import com.emailcampaign.dto.RecipientListDto;
import com.emailcampaign.model.Recipient;
import com.emailcampaign.model.RecipientList;
import com.emailcampaign.model.User;
import com.emailcampaign.service.RecipientService;
import com.emailcampaign.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recipients")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class RecipientController {
    
    private final RecipientService recipientService;
    private final UserService userService;
    
    // Recipient List endpoints
    @PostMapping("/lists")
    public ResponseEntity<ApiResponse> createRecipientList(@Valid @RequestBody RecipientListDto dto,
                                                          Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            RecipientList recipientList = recipientService.createRecipientList(dto, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Recipient list created successfully", recipientList));
        } catch (Exception e) {
            log.error("Error creating recipient list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/lists")
    public ResponseEntity<ApiResponse> getUserRecipientLists(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<RecipientList> lists = recipientService.getUserRecipientLists(user, pageable);
            return ResponseEntity.ok(ApiResponse.success("Recipient lists retrieved successfully", lists));
        } catch (Exception e) {
            log.error("Error retrieving recipient lists: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving recipient lists"));
        }
    }
    
    @GetMapping("/lists/{id}")
    public ResponseEntity<ApiResponse> getRecipientList(@PathVariable Long id,
                                                       Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Optional<RecipientList> recipientList = recipientService.findRecipientListById(id);
            
            if (recipientList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Recipient list not found"));
            }
            
            // Check access
            if (!recipientList.get().getUser().getId().equals(user.getId()) && 
                user.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Recipient list retrieved successfully", recipientList.get()));
        } catch (Exception e) {
            log.error("Error retrieving recipient list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving recipient list"));
        }
    }
    
    @PutMapping("/lists/{id}")
    public ResponseEntity<ApiResponse> updateRecipientList(@PathVariable Long id,
                                                          @Valid @RequestBody RecipientListDto dto,
                                                          Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            RecipientList recipientList = recipientService.updateRecipientList(id, dto, user);
            return ResponseEntity.ok(ApiResponse.success("Recipient list updated successfully", recipientList));
        } catch (Exception e) {
            log.error("Error updating recipient list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/lists/{id}")
    public ResponseEntity<ApiResponse> deleteRecipientList(@PathVariable Long id,
                                                          Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            recipientService.deleteRecipientList(id, user);
            return ResponseEntity.ok(ApiResponse.success("Recipient list deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting recipient list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Individual recipient endpoints
    @PostMapping
    public ResponseEntity<ApiResponse> addRecipient(@Valid @RequestBody RecipientDto dto,
                                                   Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Recipient recipient = recipientService.addRecipient(dto, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Recipient added successfully", recipient));
        } catch (Exception e) {
            log.error("Error adding recipient: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/lists/{listId}/recipients")
    public ResponseEntity<ApiResponse> getRecipientsByList(@PathVariable Long listId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          Authentication authentication) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Recipient> recipients = recipientService.getRecipientsByList(listId, pageable);
            return ResponseEntity.ok(ApiResponse.success("Recipients retrieved successfully", recipients));
        } catch (Exception e) {
            log.error("Error retrieving recipients: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving recipients"));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateRecipient(@PathVariable Long id,
                                                      @Valid @RequestBody RecipientDto dto,
                                                      Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Recipient recipient = recipientService.updateRecipient(id, dto, user);
            return ResponseEntity.ok(ApiResponse.success("Recipient updated successfully", recipient));
        } catch (Exception e) {
            log.error("Error updating recipient: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRecipient(@PathVariable Long id,
                                                      Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            recipientService.deleteRecipient(id, user);
            return ResponseEntity.ok(ApiResponse.success("Recipient deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting recipient: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // CSV Import/Export endpoints
    @PostMapping("/lists/{listId}/import")
    public ResponseEntity<ApiResponse> importRecipients(@PathVariable Long listId,
                                                       @RequestParam("file") MultipartFile file,
                                                       Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            List<Recipient> recipients = recipientService.importRecipientsFromCsv(listId, file, user);
            return ResponseEntity.ok(ApiResponse.success("Recipients imported successfully", 
                    "Imported " + recipients.size() + " recipients"));
        } catch (Exception e) {
            log.error("Error importing recipients: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/lists/{listId}/export")
    public ResponseEntity<String> exportRecipients(@PathVariable Long listId,
                                                   Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            String csvContent = recipientService.exportRecipientsToCsv(listId, user);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "recipients.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvContent);
        } catch (Exception e) {
            log.error("Error exporting recipients: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error exporting recipients");
        }
    }
    
    @PostMapping("/unsubscribe")
    public ResponseEntity<ApiResponse> unsubscribeRecipient(@RequestParam String email,
                                                           @RequestParam Long listId) {
        try {
            recipientService.unsubscribeRecipient(email, listId);
            return ResponseEntity.ok(ApiResponse.success("Unsubscribed successfully"));
        } catch (Exception e) {
            log.error("Error unsubscribing recipient: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error processing unsubscribe request"));
        }
    }
    
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
