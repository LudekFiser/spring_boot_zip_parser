package com.example.zip_project_parser_backend.github;

import com.example.zip_project_parser_backend.parser.ZipParser;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Map;

@Service
public class GithubImportService {

    private final GithubClient githubClient;
    private final ZipParser zipParser;

    public GithubImportService(GithubClient githubClient, ZipParser zipParser) {
        this.githubClient = githubClient;
        this.zipParser = zipParser;
    }

    public Map<String, String> importRepository(String url) {
        var parse = GithubParser.parse(url);
        var zip = githubClient.downloadZip(parse.owner(), parse.repo());
        return zipParser.readResources(new ByteArrayInputStream(zip));
    }
}
