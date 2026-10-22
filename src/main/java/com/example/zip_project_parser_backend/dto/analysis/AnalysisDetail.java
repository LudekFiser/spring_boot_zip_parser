package com.example.zip_project_parser_backend.dto.analysis;

import com.example.zip_project_parser_backend.dto.ProjectAnalysisResponse;

import java.util.Map;

public record AnalysisDetail(
        AnalysisSummary summary,
        ProjectAnalysisResponse analysisResponse,
        Map<String, String> sourceCode
) {
}
