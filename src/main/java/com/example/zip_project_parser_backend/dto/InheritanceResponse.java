package com.example.zip_project_parser_backend.dto;

import java.util.List;

public record InheritanceResponse(
        List<InheritanceTypeResponse> extendedTypes,
        List<InheritanceTypeResponse> implementedTypes
) {
}
