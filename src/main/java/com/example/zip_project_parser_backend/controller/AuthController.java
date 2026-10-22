package com.example.zip_project_parser_backend.controller;

import com.example.zip_project_parser_backend.dto.user.UserResponse;
import com.example.zip_project_parser_backend.service.AuthService;
import com.example.zip_project_parser_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {


    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal OAuth2User oAuth2User) {
        var user = userService.getCurrentUser(oAuth2User);
        return ResponseEntity.ok().body(user);
    }
}
