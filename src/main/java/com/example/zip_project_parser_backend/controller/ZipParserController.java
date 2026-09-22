package com.example.zip_project_parser_backend.controller;

import com.example.zip_project_parser_backend.dto.ClassNameAndFieldResponse;
import com.example.zip_project_parser_backend.dto.ClassDetailsResponse;
import com.example.zip_project_parser_backend.dto.ProjectAnalysisResponse;
import com.example.zip_project_parser_backend.parser.ZipParser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/zip")
public class ZipParserController {

    private final ZipParser zipParser;

    public ZipParserController(ZipParser zipParser) {
        this.zipParser = zipParser;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadAndParseZip(@RequestParam("zip") MultipartFile zip) {
        var method = zipParser.uploadAndParseZip(zip);
        return ResponseEntity.ok().body(method);
    }

    @PostMapping("/class-details")
    public ResponseEntity<ProjectAnalysisResponse> getClassDetails(@RequestBody @Valid Map<String, String> map) {
        var methods = zipParser.getClassDetails(map);
        return ResponseEntity.ok().body(methods);
    }
}
