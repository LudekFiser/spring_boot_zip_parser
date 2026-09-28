package com.example.zip_project_parser_backend.parser;

import com.example.zip_project_parser_backend.dto.MethodCallResponse;
import com.github.javaparser.ast.expr.MethodCallExpr;

public record CallSite(
        MethodCallExpr methodCallExpr,
        MethodCallResponse methodCallResponse
) {
}
