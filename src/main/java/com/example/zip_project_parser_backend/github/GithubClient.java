package com.example.zip_project_parser_backend.github;

import com.example.zip_project_parser_backend.exceptions.GithubRepositoryNotFoundException;
import com.example.zip_project_parser_backend.exceptions.GithubUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GithubClient {

    private final RestClient restClient;

    public GithubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] downloadZip(String owner, String repo) {
        try {
            var body = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}/zipball", owner, repo)
                    .retrieve()
                    .body(byte[].class);
            if (body == null) throw new GithubUnavailableException("Github seems to be unavailable");
            return body;
        } catch (HttpClientErrorException.NotFound e) {
            throw new GithubRepositoryNotFoundException("Github repository not found!");
        } catch (RestClientException e) {
            throw new GithubUnavailableException("Github seems to be unavailable");
        }
    }
}
