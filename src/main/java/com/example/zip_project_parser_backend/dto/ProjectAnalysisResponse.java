package com.example.zip_project_parser_backend.dto;


import java.util.List;
import java.util.Map;

public record ProjectAnalysisResponse(
        Map<String, List<ClassDetailsResponse>> classDetailsByPath,
        Map<String, List<String>> typesByPackage,
        List<PackageNodeResponse> packageTree,
        List<EndpointResponse> endpoints,
        List<MethodCallResponse> methodCalls,
        List<FieldDependencyResponse> fieldDependencies,
        List<MethodDependencyResponse> methodDependencies,
        List<ConstructorDependencyResponse> constructorDependencies,
        List<InheritanceDependencyResponse> inheritanceDependencies
) {
}
