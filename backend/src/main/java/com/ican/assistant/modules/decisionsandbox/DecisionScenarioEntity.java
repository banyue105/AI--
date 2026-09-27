package com.ican.assistant.modules.decisionsandbox;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decision_scenarios")
public class DecisionScenarioEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) String id;
    @Column(nullable = false) String title;
    @Column(nullable = false) String goal;
    @Column(name = "time_limit_days", nullable = false) int timeLimitDays;
    @Column(name = "budget_yuan", nullable = false) BigDecimal budgetYuan;
    @Column(name = "people_count", nullable = false) int peopleCount;
    @Column(name = "has_server", nullable = false) boolean hasServer;
    @Column(name = "change_request") String changeRequest;
    @Column(nullable = false) int revision;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    List<DecisionConstraintEntity> constraints = new ArrayList<>();
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    List<DecisionResourceEntity> resources = new ArrayList<>();
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    List<DecisionNodeEntity> nodes = new ArrayList<>();
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    List<DecisionRelationEntity> relations = new ArrayList<>();

    public DecisionScenarioEntity() {}
}
