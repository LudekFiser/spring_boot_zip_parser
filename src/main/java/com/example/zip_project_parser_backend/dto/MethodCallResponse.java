package com.example.zip_project_parser_backend.dto;

import com.example.zip_project_parser_backend.enumeration.MethodDependencyKind;

public record MethodCallResponse(
        String sourceType,
        String sourceMethodSignature,
        String calledMethodName,
        String scope,
        int argumentCount
) {
}
