package com.example.zip_project_parser_backend.service;

import com.example.zip_project_parser_backend.dto.user.UserResponse;
import com.example.zip_project_parser_backend.entity.User;
import com.example.zip_project_parser_backend.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AuthService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        DefaultOAuth2UserService defaultService = new DefaultOAuth2UserService();
        OAuth2User defaultOAuthUser = defaultService.loadUser(userRequest);
        Number id = defaultOAuthUser.getAttribute("id");
        String login = defaultOAuthUser.getAttribute("login");
        String avatarUrl = defaultOAuthUser.getAttribute("avatar_url");
        if (id == null || login == null) {
            throw new RuntimeException();
        }
        User user;
        var existingUser = userRepository.findByGithubId(id.longValue()).orElse(null);
        if (existingUser == null) {
            user = new User(id.longValue(), avatarUrl, login);
        } else {
            user = existingUser;
            if (!Objects.equals(user.getAvatarUrl(), avatarUrl)) {
                user.setAvatarUrl(avatarUrl);
            }
            if (!Objects.equals(user.getGithubUsername(),login)) user.setGithubUsername(login);
        }
        userRepository.save(user);
        return defaultOAuthUser;
    }
}
