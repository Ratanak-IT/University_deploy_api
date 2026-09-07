package com.universitymanagement.grading.repository;

import com.universitymanagement.grading.entity.AssessmentScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentScoreHistoryRepository extends JpaRepository<AssessmentScoreHistory, UUID> {
    List<AssessmentScoreHistory> findByAssessmentScore_ScoreIdOrderByChangedAtDesc(UUID scoreId);
}
