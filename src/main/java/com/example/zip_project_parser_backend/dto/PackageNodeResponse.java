package com.example.zip_project_parser_backend.dto;

import java.util.List;

public record PackageNodeResponse(
        String name,
        String fullName,
        List<String> typeNames,
        List<PackageNodeResponse> subPackages
) {
}
