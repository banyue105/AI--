package com.ican.assistant.modules.decisionsandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "decision_resources")
public class DecisionResourceEntity {
    @Id @Column(length = 255) String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scenario_id", nullable = false)
    DecisionScenarioEntity scenario;
    String type;
    String label;
    Integer quantity;
    String unit;
    String source;
    BigDecimal confidence;
    String assumption;

    public DecisionResourceEntity() {}
}
