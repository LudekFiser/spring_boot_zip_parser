package com.example.zip_project_parser_backend.dto;

import java.util.List;

public record MethodResponse(
        String name,
        String signature,
        String returnType,
        List<String> annotations,
        List<ParameterResponse> parameters,
        List<String> returnReferencedTypes
) {
}
