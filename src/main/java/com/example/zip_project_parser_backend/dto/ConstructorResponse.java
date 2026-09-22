package com.example.zip_project_parser_backend.dto;

import java.util.List;

public record ConstructorResponse(
        String name,
        List<String> annotations,
        List<ParameterResponse> parameters
) {
}
