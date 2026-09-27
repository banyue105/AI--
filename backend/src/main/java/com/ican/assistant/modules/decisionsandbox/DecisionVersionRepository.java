package com.ican.assistant.modules.decisionsandbox;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionVersionRepository extends JpaRepository<DecisionVersionEntity, String> {
    List<DecisionVersionEntity> findByScenario_IdOrderByVersionNumberDesc(String scenarioId);
    Optional<DecisionVersionEntity> findFirstByScenario_IdOrderByVersionNumberDesc(String scenarioId);
    long countByScenario_Id(String scenarioId);
}
