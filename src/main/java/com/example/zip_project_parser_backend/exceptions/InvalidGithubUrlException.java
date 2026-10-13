package com.example.zip_project_parser_backend.exceptions;

public class InvalidGithubUrlException extends RuntimeException {

    public InvalidGithubUrlException(String message) {
        super(message);
    }
}
