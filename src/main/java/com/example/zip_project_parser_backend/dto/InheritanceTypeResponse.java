package com.example.zip_project_parser_backend.dto;

import java.util.List;

public record InheritanceTypeResponse(
        String type,
        String baseType,
        List<String> referencedTypes
) {
}
