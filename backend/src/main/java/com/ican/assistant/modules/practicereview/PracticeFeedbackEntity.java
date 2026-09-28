package com.ican.assistant.modules.practicereview;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "practice_feedback")
public class PracticeFeedbackEntity {
    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @Column(name = "project_id", nullable = false, length = 36) String projectId;
    @Column(name = "review_id", nullable = false, length = 36) String reviewId;
    @Column(nullable = false, length = 16) String target;
    @Column(name = "target_key", nullable = false, length = 240) String targetKey;
    @Column(name = "payload_json", nullable = false, columnDefinition = "longtext") String payloadJson;
    @Column(name = "request_id", nullable = false, length = 36) String requestId;
    @Column(name = "request_hash", nullable = false, length = 64) String requestHash;
    @Column(name = "item_index", nullable = false) int itemIndex;
    @Column(name = "confirmed_at", nullable = false) Instant confirmedAt;
}
