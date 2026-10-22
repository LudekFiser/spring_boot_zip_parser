package com.example.zip_project_parser_backend.repository;

import com.example.zip_project_parser_backend.dto.analysis.AnalysisSummary;
import com.example.zip_project_parser_backend.entity.Analysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    Page<AnalysisSummary> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Analysis> findByIdAndUser_Id(UUID id, Long userId);


    @Query("""
    DELETE FROM Analysis a WHERE a.user.id = :userId AND a.id = :analysisId
    """)
    @Modifying
    int deleteAnalysisByIdAndUser_Id(@Param("analysisId") UUID analysisId, @Param("userId") Long userId);

}
