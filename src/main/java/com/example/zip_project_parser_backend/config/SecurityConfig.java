package com.example.zip_project_parser_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(CsrfConfigurer::spa)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/zip/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .defaultSuccessUrl("http://localhost:5173/", true)
                        .failureUrl("http://localhost:5173/?login=error"))
                .logout(logout -> logout
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }
}
