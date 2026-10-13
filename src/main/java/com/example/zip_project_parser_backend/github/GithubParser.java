package com.example.zip_project_parser_backend.github;

import com.example.zip_project_parser_backend.exceptions.InvalidGithubUrlException;

import java.net.URI;

public class GithubParser {

    public static UrlResponse parse(String url) {
        if (url == null || url.isBlank()) throw new InvalidGithubUrlException("Enter a valid URL!");
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidGithubUrlException("Invalid URL!");
        }
        if (!"https".equals(uri.getScheme())) throw new InvalidGithubUrlException("Invalid SCHEME: (isn't https)");
        boolean isGithub = uri.getHost() != null && ("github.com".equalsIgnoreCase(uri.getHost()) || "www.github.com".equalsIgnoreCase(uri.getHost()));
        if (!isGithub) {
            throw new InvalidGithubUrlException("This is not a github URL!");
        }
        var paths = uri.getPath().split("/");
        if (paths.length < 3) throw new InvalidGithubUrlException("Github url doesn't include profile name and/or the repository name!");
        var owner = !paths[1].isEmpty() ? paths[1] : null;
        if (owner == null) throw new InvalidGithubUrlException("Repository Owner not found in URL!");
        String repo = null;
        if (!paths[2].isEmpty()) {
            if (paths[2].endsWith(".git")) {
                repo = paths[2].substring(0, paths[2].length() - 4);
            }
            else {
                repo = paths[2];
            }
        }

        if (repo == null) throw new InvalidGithubUrlException("Repository Name not found in URL!");

        var allowedOwnerChars = "[a-zA-Z0-9-]+";
        var allowedRepoChars = "[a-zA-Z0-9-._]+";
        if (!owner.matches(allowedOwnerChars)) throw new InvalidGithubUrlException("Invalid characters in URL: (Owner name)");
        if (!repo.matches(allowedRepoChars)) throw new InvalidGithubUrlException("Invalid characters in URL: (Repository name)");
        if (repo.equals(".") || repo.equals("..")) throw new InvalidGithubUrlException("Invalid repository name");

        return new UrlResponse(owner, repo);
    }
}
