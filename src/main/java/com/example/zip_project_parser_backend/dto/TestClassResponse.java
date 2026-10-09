package com.example.zip_project_parser_backend.dto;

import com.example.zip_project_parser_backend.enumeration.TestKind;

import java.util.List;

public record TestClassResponse(
        String classFullName,
        TestKind testKind,
        List<String> testMethods,
        List<TestRequestResponse> requests
) {
}
