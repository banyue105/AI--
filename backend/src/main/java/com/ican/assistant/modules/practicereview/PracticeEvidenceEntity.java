package com.ican.assistant.modules.practicereview;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "practice_evidence")
public class PracticeEvidenceEntity {
    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id", nullable = false)
    PracticeProjectEntity project;
    @Column(nullable = false) int revision = 1;
    @Column(nullable = false, length = 240) String title;
    @Column(nullable = false, length = 16) String kind;
    @Column(nullable = false, columnDefinition = "longtext") String content;
    @Column(name = "criterion_ids_json", nullable = false, columnDefinition = "longtext") String criterionIdsJson;
    @Column(name = "updated_at", nullable = false) Instant updatedAt = Instant.now();
}
