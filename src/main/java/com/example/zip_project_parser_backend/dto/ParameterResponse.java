package com.example.zip_project_parser_backend.dto;

import java.util.List;

public record ParameterResponse(
        String name,
        String type,
        List<String> annotations,
        List<String> referencedTypes
) {
}
