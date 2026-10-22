package com.example.zip_project_parser_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ZipProjectParserBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZipProjectParserBackendApplication.class, args);
    }

}
