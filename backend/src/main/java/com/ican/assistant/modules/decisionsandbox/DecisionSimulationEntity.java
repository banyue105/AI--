package com.ican.assistant.modules.decisionsandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "decision_simulations")
public class DecisionSimulationEntity {
    @Id String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "version_id", nullable = false)
    DecisionVersionEntity version;
    @Column(name = "option_key") String optionKey;
    @Column(name = "result_json", columnDefinition = "text") String resultJson;

    public DecisionSimulationEntity() {}
}
