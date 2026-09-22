package com.example.zip_project_parser_backend.dto;

import com.example.zip_project_parser_backend.enumeration.MethodDependencyKind;

public record MethodDependencyResponse(
        String sourceType,
        String methodName,
        String methodSignature,
        String targetType,
        MethodDependencyKind dependencyKind,
        String parameterName
) {
}
