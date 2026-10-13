package com.example.zip_project_parser_backend.github;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GithubClient {

    private final RestClient restClient;

    public GithubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] downloadZip(String owner, String repo) {
        return restClient
                .get()
                .uri("/repos/{owner}/{repo}/zipball", owner, repo)
                .retrieve()
                .body(byte[].class);
    }
}
