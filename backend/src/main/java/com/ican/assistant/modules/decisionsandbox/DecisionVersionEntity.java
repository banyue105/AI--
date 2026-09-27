package com.ican.assistant.modules.decisionsandbox;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decision_versions")
public class DecisionVersionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scenario_id", nullable = false)
    DecisionScenarioEntity scenario;
    @Column(name = "scenario_id", insertable = false, updatable = false) String scenarioId;
    @Column(name = "version_number") int versionNumber;
    @Column(name = "scenario_revision") int scenarioRevision;
    @Column(name = "change_summary") String changeSummary;
    @Column(name = "snapshot_json", columnDefinition = "text") String snapshotJson;
    @Column(name = "created_at") Instant createdAt;
    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    List<DecisionSimulationEntity> simulations = new ArrayList<>();

    public DecisionVersionEntity() {}
}
