package com.ican.assistant.modules.practicereview;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "practice_criteria")
public class PracticeCriterionEntity {
    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id", nullable = false)
    PracticeProjectEntity project;
    @Column(nullable = false, length = 240) String title;
    @Column(nullable = false, length = 2000) String standard;
    @Column(name = "expected_evidence", nullable = false, length = 2000) String expectedEvidence;
    @Column(nullable = false) boolean required;
    @Column(name = "order_index", nullable = false) int orderIndex;
}
