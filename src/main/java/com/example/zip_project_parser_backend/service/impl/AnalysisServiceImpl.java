package com.example.zip_project_parser_backend.service.impl;

import com.example.zip_project_parser_backend.dto.analysis.AnalysisDetail;
import com.example.zip_project_parser_backend.dto.analysis.AnalysisSummary;
import com.example.zip_project_parser_backend.dto.analysis.CreateAnalysisRequest;
import com.example.zip_project_parser_backend.entity.Analysis;
import com.example.zip_project_parser_backend.enumeration.Source;
import com.example.zip_project_parser_backend.exceptions.AnalysisNotFoundException;
import com.example.zip_project_parser_backend.exceptions.InvalidGithubUrlException;
import com.example.zip_project_parser_backend.parser.ZipParser;
import com.example.zip_project_parser_backend.repository.AnalysisRepository;
import com.example.zip_project_parser_backend.service.AnalysisService;
import com.example.zip_project_parser_backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final UserService userService;
    private final AnalysisRepository analysisRepository;
    private final ZipParser zipParser;

    public AnalysisServiceImpl(UserService userService, AnalysisRepository analysisRepository, ZipParser zipParser) {
        this.userService = userService;
        this.analysisRepository = analysisRepository;
        this.zipParser = zipParser;
    }

    @Override
    public AnalysisSummary saveAnalysis(CreateAnalysisRequest request) {
        var currentUser = userService.getLoggedInUser();
        if (request.source() == Source.GITHUB && request.githubUrl().isBlank()) {
            throw new InvalidGithubUrlException("If you are importing from github, Github URL is required!");
        }
        String githubUrl = null;
        if (request.source() == Source.GITHUB && !request.githubUrl().isBlank()) {
            githubUrl = request.githubUrl();
        }

        var analysis = zipParser.getClassDetails(request.sourceCode());

        var analysisEntity = new Analysis(
                githubUrl, request.projectName(),
                request.source(), request.sourceCode());
        analysisEntity.setUser(currentUser);
        analysisEntity.setJsonAnalysis(analysis);
        var saved = analysisRepository.save(analysisEntity);
        return AnalysisSummary.from(saved);
    }

    @Override
    public AnalysisDetail getAnalysisById(UUID analysisId) {
        var currentUser = userService.getLoggedInUser();

        var analysis =  analysisRepository.findByIdAndUser_Id(analysisId, currentUser.getId())
                .orElseThrow(() -> new AnalysisNotFoundException("Analysis was not found for given id and currently logged in user"));

        var analysisSummary = AnalysisSummary.from(analysis);

        return new AnalysisDetail(analysisSummary, analysis.getJsonAnalysis() ,analysis.getSourceCode());
    }

    @Override
    public Page<AnalysisSummary> getMyAnalyses(Pageable pageable) {
        var currentUser = userService.getLoggedInUser();
        return analysisRepository.findAllByUser_IdOrderByCreatedAtDesc(currentUser.getId(), pageable);
    }

    @Override
    @Transactional
    public void deleteAnalysis(UUID analysisId) {
        var currentUser = userService.getLoggedInUser();

        var deletions = analysisRepository.deleteAnalysisByIdAndUser_Id(analysisId, currentUser.getId());
        if (deletions == 0) {
            throw new AnalysisNotFoundException("There is no such analysis to delete");
        }
    }
}
