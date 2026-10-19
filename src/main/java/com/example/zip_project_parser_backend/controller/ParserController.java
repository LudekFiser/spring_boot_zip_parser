package com.example.zip_project_parser_backend.controller;

import com.example.zip_project_parser_backend.dto.ProjectAnalysisResponse;
import com.example.zip_project_parser_backend.github.GithubImportRequest;
import com.example.zip_project_parser_backend.github.GithubImportService;
import com.example.zip_project_parser_backend.parser.ZipParser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/zip")
public class ParserController {

    private final ZipParser zipParser;
    private final GithubImportService githubImportService;

    public ParserController(ZipParser zipParser, GithubImportService githubImportService) {
        this.zipParser = zipParser;
        this.githubImportService = githubImportService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadAndParseZip(@RequestParam("zip") MultipartFile zip) {
        var method = zipParser.uploadAndParseZip(zip);
        return ResponseEntity.ok().body(method);
    }

    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> uploadAndParseGithubUrl(@RequestBody @Valid GithubImportRequest request) {
        var repository = githubImportService.importRepository(request.url());
        return ResponseEntity.ok().body(repository);
    }

    @PostMapping("/class-details")
    public ResponseEntity<ProjectAnalysisResponse> getClassDetails(@RequestBody @Valid Map<String, String> map) {
        var methods = zipParser.getClassDetails(map);
        return ResponseEntity.ok().body(methods);
    }
}
