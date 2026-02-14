package com.shkgroups.security;

import com.shkgroups.config.ApiKeyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyProperties apiKeyProps;

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter(apiKeyProps);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/pair/**", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v1/payments/mercadopago/notification").permitAll()
                        .requestMatchers("/v1/**").hasRole("SERVICE")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                        .accessDeniedHandler((req, res, ex) -> res.sendError(403))
                )
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .build();
    }

}
