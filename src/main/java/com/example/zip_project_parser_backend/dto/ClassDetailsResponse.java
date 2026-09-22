package com.example.zip_project_parser_backend.dto;

import java.util.List;

public record ClassDetailsResponse(
        String packageName,
        List<MethodResponse> methodResponseList,
        List<FieldResponse> fieldResponseList,
        List<ConstructorResponse> constructorResponseList,
        InheritanceResponse inheritanceResponse,
        ClassNameResponse classNameResponse
) {
}
