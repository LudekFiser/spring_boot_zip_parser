package com.example.zip_project_parser_backend.parser;

import com.github.javaparser.ast.body.TypeDeclaration;

public record PendingTest(
        TypeDeclaration<?> type,
        String classFullName
) {
}
