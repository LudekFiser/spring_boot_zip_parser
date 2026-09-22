package com.example.zip_project_parser_backend.dto;

import com.example.zip_project_parser_backend.enumeration.ClassKind;

import java.util.List;

public record ClassNameResponse(
        String className,
        ClassKind kind,
        List<String> constants,
        List<String> annotations
) {
}
