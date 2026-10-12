package com.example.zip_project_parser_backend.github;

import java.net.URI;

public class GithubParser {

    public static UrlResponse parse(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Enter a valid URL!");
        var uri = URI.create(url.trim());
        if (!"https".equals(uri.getScheme())) throw new IllegalArgumentException("Invalid SCHEME: (isn't https)");
        boolean isGithub = uri.getHost() != null && ("github.com".equalsIgnoreCase(uri.getHost()) || "www.github.com".equalsIgnoreCase(uri.getHost()));
        if (!isGithub) {
            throw new IllegalArgumentException("This is not a github URL!");
        }
        var paths = uri.getPath().split("/");
        if (paths.length < 3) throw new IllegalArgumentException("Github url doesn't include profile name and/or the repository name!");
        var owner = !paths[1].isEmpty() ? paths[1] : null;
        if (owner == null) throw new IllegalArgumentException("Repository Owner not found in URL!");
        String repo = null;
        if (!paths[2].isEmpty()) {
            if (paths[2].endsWith(".git")) {
                repo = paths[2].substring(0, paths[2].length() - 4);
            }
            else {
                repo = paths[2];
            }
        }

        if (repo == null) throw new IllegalArgumentException("Repository Name not found in URL!");

        var allowedOwnerChars = "[a-zA-Z0-9-]+";
        var allowedRepoChars = "[a-zA-Z0-9-._]+";
        if (!owner.matches(allowedOwnerChars)) throw new IllegalArgumentException("Invalid characters in URL: (Owner name)");
        if (!repo.matches(allowedRepoChars)) throw new IllegalArgumentException("Invalid characters in URL: (Repository name)");
        if (repo.equals(".") || repo.equals("..")) throw new IllegalArgumentException("Invalid repository name");

        return new UrlResponse(owner, repo);

    }
}
