package com.example.zip_project_parser_backend.exceptions;

public class GithubRepositoryNotFoundException extends RuntimeException {
    public GithubRepositoryNotFoundException(String message) {
        super(message);
    }
}
