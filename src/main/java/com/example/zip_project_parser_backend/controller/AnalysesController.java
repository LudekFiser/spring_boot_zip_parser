package com.example.zip_project_parser_backend.controller;

import com.example.zip_project_parser_backend.dto.analysis.AnalysisDetail;
import com.example.zip_project_parser_backend.dto.analysis.AnalysisSummary;
import com.example.zip_project_parser_backend.dto.analysis.CreateAnalysisRequest;
import com.example.zip_project_parser_backend.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.UUID;

@RestController
@RequestMapping("/analyses")
public class AnalysesController {

    private final AnalysisService analysisService;

    public AnalysesController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<AnalysisSummary> saveAnalysis(@Valid @RequestBody CreateAnalysisRequest request) {
        var analysis = analysisService.saveAnalysis(request);
        var uri = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(analysis.id())
                .toUri();
        return ResponseEntity.created(uri).body(analysis);
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisDetail> getAnalysisById(@PathVariable UUID analysisId) {
        var analysis = analysisService.getAnalysisById(analysisId);
        return ResponseEntity.ok().body(analysis);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<AnalysisSummary>> getMyAnalyses(Pageable pageable) {
        var analyses = analysisService.getMyAnalyses(pageable);
        return ResponseEntity.ok().body(analyses);
    }

    @DeleteMapping("/{analysisId}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable UUID analysisId) {
        analysisService.deleteAnalysis(analysisId);
        return ResponseEntity.noContent().build();
    }
}
