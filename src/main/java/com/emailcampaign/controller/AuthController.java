package com.emailcampaign.controller;

import com.emailcampaign.dto.ApiResponse;
import com.emailcampaign.dto.LoginDto;
import com.emailcampaign.dto.UserRegistrationDto;
import com.emailcampaign.model.User;
import com.emailcampaign.service.SystemLogService;
import com.emailcampaign.service.UserService;
import com.emailcampaign.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final SystemLogService systemLogService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginDto loginDto, 
                                           HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            User user = userService.findByUsername(loginDto.getUsername()).orElse(null);
            if (user != null) {
                userService.updateLastLogin(loginDto.getUsername());
                systemLogService.logUserAction(user, "USER_LOGIN", 
                        "User logged in successfully", 
                        request.getRemoteAddr(), 
                        request.getHeader("User-Agent"));
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", jwt);
            responseData.put("user", user);
            return ResponseEntity.ok(ApiResponse.success("Login successful", responseData));
            
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginDto.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody UserRegistrationDto registrationDto,
                                              HttpServletRequest request) {
        try {
            User user = userService.registerUser(registrationDto);
            
            systemLogService.logUserAction(user, "USER_REGISTRATION", 
                    "User registered successfully", 
                    request.getRemoteAddr(), 
                    request.getHeader("User-Agent"));
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully", user.getId()));
                    
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestParam String email) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Password reset instructions sent to email"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error processing password reset request"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}
