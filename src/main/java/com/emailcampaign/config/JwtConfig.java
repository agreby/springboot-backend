package com.emailcampaign.config;

import com.emailcampaign.security.JwtAuthenticationFilter;
import com.emailcampaign.security.JwtTokenProvider;
import com.emailcampaign.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserService userService) {
        return new JwtAuthenticationFilter(tokenProvider, userService);
    }
} 