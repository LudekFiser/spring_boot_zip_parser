package com.example.zip_project_parser_backend.github;

import com.example.zip_project_parser_backend.exceptions.GithubRepositoryNotFoundException;
import com.example.zip_project_parser_backend.exceptions.GithubUnavailableException;
import com.example.zip_project_parser_backend.exceptions.ProjectTooLargeException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GithubClient {

    private final RestClient restClient;
    private static final int MAX_ZIP_BYTES = 100_000;//100_000_000;

    public GithubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] downloadZip(String owner, String repo) {
        try {

            return restClient
                    .get()
                    .uri("/repos/{owner}/{repo}/zipball", owner, repo)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                            throw new GithubRepositoryNotFoundException("Github repository not found!");
                        }
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new GithubUnavailableException("Github seems to be unavailable");
                        }
                        var byteSize = response.getBody().readNBytes(MAX_ZIP_BYTES + 1);
                        if (byteSize.length <= MAX_ZIP_BYTES) {
                            return byteSize;
                        } else {
                            throw new ProjectTooLargeException("Github repository exceeded the maximum limit of %,d MB".formatted(MAX_ZIP_BYTES / 1000000));
                        }
                    });
        } catch (RestClientException e) {
            throw new GithubUnavailableException("Github seems to be unavailable");
        }
    }
}
