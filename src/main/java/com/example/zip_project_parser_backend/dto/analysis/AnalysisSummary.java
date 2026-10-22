package com.example.zip_project_parser_backend.dto.analysis;

import com.example.zip_project_parser_backend.entity.Analysis;
import com.example.zip_project_parser_backend.enumeration.Source;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnalysisSummary(
        UUID id,
        String projectName,
        Source source,
        String githubUrl,
        OffsetDateTime createdAt
) {

    public static AnalysisSummary from(Analysis analysis) {
        return new AnalysisSummary(analysis.getId(), analysis.getProjectName(), analysis.getSource(),
                analysis.getGithubUrl(), analysis.getCreatedAt());
    }
}
