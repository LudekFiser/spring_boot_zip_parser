package com.example.zip_project_parser_backend.dto;

public record FieldDependencyResponse(
        String sourceType,
        String fieldName,
        String targetType
) {
}
