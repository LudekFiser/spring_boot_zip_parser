package com.example.zip_project_parser_backend.dto;


public record MethodCallResponse(
        String sourceType,
        String sourceMethodSignature,
        String calledMethodName,
        String scope,
        String scopeType,
        String targetType,
        String targetMethodSignature,
        int argumentCount
) {
}
