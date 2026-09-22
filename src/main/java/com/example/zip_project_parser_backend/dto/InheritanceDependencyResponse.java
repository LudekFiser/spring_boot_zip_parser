package com.example.zip_project_parser_backend.dto;

import com.example.zip_project_parser_backend.enumeration.InheritanceDependencyKind;

import java.util.List;

public record InheritanceDependencyResponse(
        String sourceType,
        String targetType,
        InheritanceDependencyKind inheritanceDependencyKind
) {
}
