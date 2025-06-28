package com.emailcampaign.controller;

import com.emailcampaign.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow all origins for tracking pixels
public class TrackingController {
    
    private final TrackingService trackingService;
    
    @GetMapping("/pixel/{trackingId}")
    public ResponseEntity<byte[]> trackEmailOpen(@PathVariable String trackingId,
                                                HttpServletRequest request) {
        try {
            trackingService.trackEmailOpen(trackingId, request);
            
            // Return a 1x1 transparent pixel
            byte[] pixel = Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
            );
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(pixel);
                    
        } catch (Exception e) {
            log.error("Error tracking email open: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/click/{trackingToken}")
    public ResponseEntity<Void> trackLinkClick(@PathVariable String trackingToken,
                                              HttpServletRequest request) {
        try {
            trackingService.trackLinkClick(trackingToken, request);
            
            // Decode the original URL and redirect
            String decodedToken = new String(Base64.getDecoder().decode(trackingToken));
            String[] parts = decodedToken.split(":");
            
            if (parts.length >= 3) {
                String originalUrl = parts[2];
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", originalUrl)
                        .build();
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            
        } catch (Exception e) {
            log.error("Error tracking link click: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/unsubscribe")
    public ResponseEntity<String> trackUnsubscribe(@RequestParam String token,
                                                  HttpServletRequest request) {
        try {
            trackingService.trackUnsubscribe(token, request);
            
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Unsubscribed</title>
                        <style>
                            body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                            .container { max-width: 600px; margin: 0 auto; }
                            .success { color: #28a745; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <h1 class="success">Successfully Unsubscribed</h1>
                            <p>You have been successfully unsubscribed from our mailing list.</p>
                            <p>You will no longer receive emails from this campaign.</p>
                        </div>
                    </body>
                    </html>
                    """;
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
                    
        } catch (Exception e) {
            log.error("Error processing unsubscribe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing unsubscribe request");
        }
    }
}
