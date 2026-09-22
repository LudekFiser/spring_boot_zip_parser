package com.example.zip_project_parser_backend.dto;

import com.example.zip_project_parser_backend.enumeration.HttpMethodKind;

import java.util.List;

public record EndpointResponse(
        String declaringType,
        String methodName,
        String methodSignature,
        HttpMethodKind httpMethod,
        String classPath,
        String methodPath,
        String fullPath,
        List<String> consumes,
        List<String> produces
) {
}
