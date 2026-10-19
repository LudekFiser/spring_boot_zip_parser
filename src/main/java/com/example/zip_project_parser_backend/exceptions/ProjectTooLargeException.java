package com.example.zip_project_parser_backend.exceptions;

public class ProjectTooLargeException extends RuntimeException {
    public ProjectTooLargeException(String message) {
        super(message);
    }
}
