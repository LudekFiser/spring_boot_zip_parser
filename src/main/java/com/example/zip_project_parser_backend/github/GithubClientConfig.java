package com.example.zip_project_parser_backend.github;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class GithubClientConfig {

    @Bean
    public RestClient githubRestClient() {
        return RestClient.builder().baseUrl("https://api.github.com")
                .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
                .build();
    }
}
