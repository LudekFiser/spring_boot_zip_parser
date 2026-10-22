package com.example.zip_project_parser_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(name = "avatar_url", length = Integer.MAX_VALUE)
    private String avatarUrl;

    @NotNull
    @Column(name = "github_id", nullable = false)
    private Long githubId;

    @Column(name = "created_at")
    @CreationTimestamp
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Long getGithubId() {
        return githubId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


    protected User() {}

    public User(Long githubId, String avatarUrl, String githubUsername) {
        this.githubId = githubId;
        this.avatarUrl = avatarUrl;
        this.githubUsername = githubUsername;
    }
}