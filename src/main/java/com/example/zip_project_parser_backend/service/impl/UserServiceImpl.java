package com.example.zip_project_parser_backend.service.impl;

import com.example.zip_project_parser_backend.dto.user.UserResponse;
import com.example.zip_project_parser_backend.repository.UserRepository;
import com.example.zip_project_parser_backend.service.UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(OAuth2User oAuth2User) {
        Number id = oAuth2User.getAttribute("id");
        if (id == null) {
            throw new RuntimeException();
        }
        var existingUser = userRepository.findByGithubId(id.longValue()).orElseThrow();

        return new UserResponse(existingUser.getGithubUsername(), existingUser.getAvatarUrl());
    }
}
