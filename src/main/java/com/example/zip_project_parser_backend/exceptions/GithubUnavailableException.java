package com.example.zip_project_parser_backend.exceptions;

public class GithubUnavailableException extends RuntimeException {
    public GithubUnavailableException(String message) {
        super(message);
    }
}
