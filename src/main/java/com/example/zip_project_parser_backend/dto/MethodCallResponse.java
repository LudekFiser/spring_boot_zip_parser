package com.example.zip_project_parser_backend.dto;


import com.example.zip_project_parser_backend.enumeration.CallResolution;

public record MethodCallResponse(
        CallResolution callResolution,
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
