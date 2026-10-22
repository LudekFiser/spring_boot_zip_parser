package com.example.zip_project_parser_backend.service;

import com.example.zip_project_parser_backend.dto.analysis.AnalysisDetail;
import com.example.zip_project_parser_backend.dto.analysis.AnalysisSummary;
import com.example.zip_project_parser_backend.dto.analysis.CreateAnalysisRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AnalysisService {


    AnalysisSummary saveAnalysis(CreateAnalysisRequest request);
    AnalysisDetail getAnalysisById(UUID analysisId);
    Page<AnalysisSummary> getMyAnalyses(Pageable pageable);
    void deleteAnalysis(UUID analysisId);
}
