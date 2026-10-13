package com.example.zip_project_parser_backend.controller;

import com.example.zip_project_parser_backend.dto.ProjectAnalysisResponse;
import com.example.zip_project_parser_backend.github.GithubClient;
import com.example.zip_project_parser_backend.github.GithubImportRequest;
import com.example.zip_project_parser_backend.github.GithubParser;
import com.example.zip_project_parser_backend.parser.ZipParser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;

@RestController
@RequestMapping("/zip")
public class ParserController {

    private final ZipParser zipParser;
    private final GithubClient githubClient;

    public ParserController(ZipParser zipParser, GithubClient githubClient) {
        this.zipParser = zipParser;
        this.githubClient = githubClient;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadAndParseZip(@RequestParam("zip") MultipartFile zip) {
        var method = zipParser.uploadAndParseZip(zip);
        return ResponseEntity.ok().body(method);
    }

    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> uploadAndParseGithubUrl(@RequestBody @Valid GithubImportRequest request) {
        var parse = GithubParser.parse(request.url());
        var zip = githubClient.downloadZip(parse.owner(), parse.repo());
        var read = zipParser.readResources(new ByteArrayInputStream(zip));
        return ResponseEntity.ok().body(read);
    }

    @PostMapping("/class-details")
    public ResponseEntity<ProjectAnalysisResponse> getClassDetails(@RequestBody @Valid Map<String, String> map) {
        var methods = zipParser.getClassDetails(map);
        return ResponseEntity.ok().body(methods);
    }
}
