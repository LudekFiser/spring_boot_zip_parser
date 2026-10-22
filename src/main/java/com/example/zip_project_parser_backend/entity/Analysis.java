package com.example.zip_project_parser_backend.entity;
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "analyses")
public class Analysis {
@jakarta.persistence.Id
@org.hibernate.annotations.ColumnDefault("gen_random_uuid()")
@jakarta.persistence.Column(name = "id", nullable = false)
private java.util.UUID id;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "user_id", nullable = false)
private com.example.zip_project_parser_backend.entity.User user;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.Column(name = "project_name", nullable = false, length = Integer.MAX_VALUE)
private java.lang.String projectName;

@jakarta.validation.constraints.NotNull
@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
@jakarta.persistence.Column(name = "json_analysis", nullable = false)
private java.util.Map<java.lang.String, java.lang.Object> jsonAnalysis;

@jakarta.validation.constraints.NotNull
@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
@jakarta.persistence.Column(name = "source_code", nullable = false)
private java.util.Map<java.lang.String, java.lang.Object> sourceCode;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.Column(name = "source", nullable = false, length = Integer.MAX_VALUE)
private java.lang.String source;

@jakarta.persistence.Column(name = "github_url", length = Integer.MAX_VALUE)
private java.lang.String githubUrl;

@jakarta.validation.constraints.NotNull
@org.hibernate.annotations.ColumnDefault("now()")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.OffsetDateTime createdAt;

public java.util.UUID getId() {
  return id;
}public void setId(java.util.UUID id) {
  this.id = id;
}

public com.example.zip_project_parser_backend.entity.User getUser() {
  return user;
}public void setUser(com.example.zip_project_parser_backend.entity.User user) {
  this.user = user;
}

public java.lang.String getProjectName() {
  return projectName;
}public void setProjectName(java.lang.String projectName) {
  this.projectName = projectName;
}

public java.util.Map<java.lang.String, java.lang.Object> getJsonAnalysis() {
  return jsonAnalysis;
}public void setJsonAnalysis(java.util.Map<java.lang.String, java.lang.Object> jsonAnalysis) {
  this.jsonAnalysis = jsonAnalysis;
}

public java.util.Map<java.lang.String, java.lang.Object> getSourceCode() {
  return sourceCode;
}public void setSourceCode(java.util.Map<java.lang.String, java.lang.Object> sourceCode) {
  this.sourceCode = sourceCode;
}

public java.lang.String getSource() {
  return source;
}public void setSource(java.lang.String source) {
  this.source = source;
}

public java.lang.String getGithubUrl() {
  return githubUrl;
}public void setGithubUrl(java.lang.String githubUrl) {
  this.githubUrl = githubUrl;
}

public java.time.OffsetDateTime getCreatedAt() {
  return createdAt;
}public void setCreatedAt(java.time.OffsetDateTime createdAt) {
  this.createdAt = createdAt;
}

}