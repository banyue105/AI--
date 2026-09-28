package com.ican.assistant.modules.practicereview;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeReviewRepository extends JpaRepository<PracticeReviewEntity, String> {
    Optional<PracticeReviewEntity> findByRequestId(String requestId);
    Optional<PracticeReviewEntity> findFirstByProjectIdOrderByReviewNumberDesc(String projectId);
    List<PracticeReviewEntity> findByProjectIdAndExecutionStatusOrderByReviewNumberDesc(String projectId, String executionStatus);
    long countByProjectIdAndExecutionStatus(String projectId, String executionStatus);
}
