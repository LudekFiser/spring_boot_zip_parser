package com.example.zip_project_parser_backend.service;

import com.example.zip_project_parser_backend.dto.user.UserResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface UserService {


    UserResponse getCurrentUser(OAuth2User oAuth2User);
}
