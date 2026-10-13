package com.example.zip_project_parser_backend.github;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class GithubClientConfig {

    @Bean
    public RestClient githubRestClient() {
        var httpClient = HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        var jdkClientFactory = new JdkClientHttpRequestFactory(httpClient);

        jdkClientFactory.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder().baseUrl("https://api.github.com")
                .requestFactory(jdkClientFactory)
                .build();
    }
}
