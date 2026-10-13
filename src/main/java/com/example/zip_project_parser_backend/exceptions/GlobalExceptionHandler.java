package com.example.zip_project_parser_backend.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {




    @ExceptionHandler(InvalidGithubUrlException.class)
    public ProblemDetail invalidGithubUrlException(InvalidGithubUrlException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), e.getMessage());
    }

    @ExceptionHandler(GithubRepositoryNotFoundException.class)
    public ProblemDetail githubRepositoryNotFound(GithubRepositoryNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), e.getMessage());
    }

    @ExceptionHandler(GithubUnavailableException.class)
    public ProblemDetail githubUnavailable(GithubUnavailableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(503), e.getMessage());
    }
}
