package com.example.zip_project_parser_backend.entity;

import com.example.zip_project_parser_backend.dto.ProjectAnalysisResponse;
import com.example.zip_project_parser_backend.enumeration.Source;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "analyses")
public class Analysis {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "project_name")
    private String projectName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_analysis")
    private ProjectAnalysisResponse jsonAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_code")
    private Map<String, String> sourceCode;

    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    private Source source;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "created_at")
    @CreationTimestamp
    private OffsetDateTime createdAt;

    public Analysis(String githubUrl, @NotBlank String projectName, @NotNull Source source, @NotEmpty Map<@NotNull String, @NotNull String> sourceCode) {
        this.githubUrl = githubUrl;
        this.projectName = projectName;
        this.source = source;
        this.sourceCode = sourceCode;
    }

    protected Analysis() {}

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public ProjectAnalysisResponse getJsonAnalysis() {
        return jsonAnalysis;
    }

    public void setJsonAnalysis(ProjectAnalysisResponse jsonAnalysis) {
        this.jsonAnalysis = jsonAnalysis;
    }

    public Map<String, String> getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(Map<String, String> sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


}