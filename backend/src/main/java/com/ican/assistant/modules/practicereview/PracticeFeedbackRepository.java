package com.ican.assistant.modules.practicereview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeFeedbackRepository extends JpaRepository<PracticeFeedbackEntity, String> {
    List<PracticeFeedbackEntity> findByRequestIdOrderByItemIndex(String requestId);
    List<PracticeFeedbackEntity> findByProjectIdOrderByConfirmedAtDescItemIndexAsc(String projectId);
    boolean existsByReviewIdAndTargetAndTargetKey(String reviewId, String target, String targetKey);
}
