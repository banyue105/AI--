package com.ican.assistant.modules.practicereview;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "practice_projects")
public class PracticeProjectEntity {
    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    // H2 measures VARCHAR in UTF-16 units; API limits remain Unicode code points.
    @Column(nullable = false, length = 240) String title;
    @Column(nullable = false, length = 2000) String goal;
    @Column(name = "template_id", nullable = false, length = 64) String templateId;
    @Column(name = "input_revision", nullable = false) int inputRevision = 1;
    @Column(name = "metrics_json", nullable = false, columnDefinition = "longtext") String metricsJson = "[]";
    @Column(name = "process_note", nullable = false, length = 10000) String processNote = "";
    @Column(name = "decision_origin_json", columnDefinition = "longtext") String decisionOriginJson;
    @Column(name = "created_at", nullable = false) Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) Instant updatedAt = createdAt;
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PracticeCriterionEntity> criteria = new ArrayList<>();
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PracticeEvidenceEntity> evidence = new ArrayList<>();
}
