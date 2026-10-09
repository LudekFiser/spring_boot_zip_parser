package com.example.zip_project_parser_backend.dto;

import com.example.zip_project_parser_backend.enumeration.HttpMethodKind;

import java.util.List;

public record TestRequestResponse(
        String testMethod,
        HttpMethodKind httpMethodKind,
        String path
) {
}
