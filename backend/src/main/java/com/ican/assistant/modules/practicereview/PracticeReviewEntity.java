package com.ican.assistant.modules.practicereview;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "practice_reviews")
public class PracticeReviewEntity {
    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @Column(name = "project_id", nullable = false, length = 36) String projectId;
    @Column(name = "review_number", nullable = false) int reviewNumber;
    @Column(name = "input_revision", nullable = false) int inputRevision;
    @Column(name = "execution_status", nullable = false, length = 16) String executionStatus;
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "longtext") String snapshotJson;
    @Column(name = "result_json", columnDefinition = "longtext") String resultJson;
    @Column(name = "confirmation_json", columnDefinition = "longtext") String confirmationJson;
    @Column(length = 8) String source;
    @Column(name = "provider_notice", length = 1000) String providerNotice;
    @Column(name = "request_id", nullable = false, length = 36) String requestId;
    @Column(name = "request_hash", nullable = false, length = 64) String requestHash;
    @Column(name = "attempt_id", nullable = false, length = 36) String attemptId;
    @Column(name = "started_at", nullable = false) Instant startedAt;
    @Column(name = "finished_at") Instant finishedAt;
}
