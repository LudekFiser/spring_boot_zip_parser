package com.example.zip_project_parser_backend.dto.analysis;

import com.example.zip_project_parser_backend.enumeration.Source;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateAnalysisRequest(
        @NotBlank String projectName,
        @NotNull Source source,
        String githubUrl,
        @NotEmpty Map<@NotNull String,@NotNull String> sourceCode
) {
}
