package com.example.zip_project_parser_backend.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class AuthService extends DefaultOAuth2UserService {

    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        DefaultOAuth2UserService defaultService = new DefaultOAuth2UserService();
        OAuth2User defaultOAuthUser = defaultService.loadUser(userRequest);
        Number id = defaultOAuthUser.getAttribute("id");
        String login = defaultOAuthUser.getAttribute("login");
        String avatarUrl = defaultOAuthUser.getAttribute("avatar_url");
        if (id == null || login == null) {
            throw new RuntimeException();
        }
        userService.syncGithubUser(id.longValue(), avatarUrl, login);
        return defaultOAuthUser;
    }
}
