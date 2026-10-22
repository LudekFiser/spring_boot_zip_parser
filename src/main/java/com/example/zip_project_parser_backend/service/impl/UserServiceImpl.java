package com.example.zip_project_parser_backend.service.impl;

import com.example.zip_project_parser_backend.dto.user.UserResponse;
import com.example.zip_project_parser_backend.entity.User;
import com.example.zip_project_parser_backend.exceptions.UserNotFoundException;
import com.example.zip_project_parser_backend.repository.UserRepository;
import com.example.zip_project_parser_backend.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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
        var existingUser = userRepository.findByGithubId(id.longValue())
                .orElseThrow(() -> new UserNotFoundException("User account not found, sign in again"));

        return new UserResponse(existingUser.getGithubUsername(), existingUser.getAvatarUrl());
    }

    @Override
    public User getLoggedInUser() {
        var authUser = (OAuth2User) Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        Number githubId = authUser.getAttribute("id");
        if (githubId == null) throw new UserNotFoundException("User account not found");
        return userRepository.findByGithubId(githubId.longValue())
                .orElseThrow(() -> new UserNotFoundException("User account not found, sign in again"));
    }

    @Override
    @Transactional
    public void syncGithubUser(Long id, String avatarUrl, String login) {
        User user;
        var existingUser = userRepository.findByGithubId(id).orElse(null);
        if (existingUser == null) {
            user = new User(id, avatarUrl, login);
        } else {
            user = existingUser;
            user.setAvatarUrl(avatarUrl);
            user.setGithubUsername(login);
        }
        userRepository.save(user);
    }
}
