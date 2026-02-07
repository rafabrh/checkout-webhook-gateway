package com.shkgroups.security;

import com.shkgroups.config.ApiKeyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyProperties apiKeyProps;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var apiKeyFilter = new ApiKeyAuthFilter(apiKeyProps);

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/pair/**", "/public/**", "/dev/**", "/webhooks/**").permitAll()
                        .requestMatchers("/v1/**", "/actuator/**").permitAll() // protegido pelo ApiKeyAuthFilter
                        .anyRequest().denyAll()
                )
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}