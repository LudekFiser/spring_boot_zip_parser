package com.example.zip_project_parser_backend.dto;

import java.util.List;

public record ClassNameAndFieldResponse(
        String packageName,
        List<FieldResponse> fieldResponseList,
        ClassNameResponse classNameResponse

) {
}
