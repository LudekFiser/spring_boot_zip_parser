package com.example.zip_project_parser_backend.github;

import jakarta.validation.constraints.NotBlank;

public record GithubImportRequest(
        @NotBlank String url
) {
}
