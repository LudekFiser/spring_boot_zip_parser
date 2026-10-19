package com.example.zip_project_parser_backend.github;

import jakarta.validation.constraints.NotBlank;

public record GithubImportRequest(
        @NotBlank(message = "Url cannot be empty!") String url
) {
}
