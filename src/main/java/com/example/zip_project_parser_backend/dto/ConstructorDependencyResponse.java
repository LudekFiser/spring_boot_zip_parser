package com.example.zip_project_parser_backend.dto;

import com.example.zip_project_parser_backend.enumeration.MethodDependencyKind;

public record ConstructorDependencyResponse(
        String sourceType,
        String constructorName,
        String targetType,
        String parameterName
) {
}
