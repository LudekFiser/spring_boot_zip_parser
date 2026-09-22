package com.example.zip_project_parser_backend.dto;

public record ImportResponse(
        String name,
        boolean isStatic,
        boolean isAsterisk,
        boolean isModule
) {
}
