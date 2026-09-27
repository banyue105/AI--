package com.ican.assistant.modules.decisionsandbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionScenarioRepository extends JpaRepository<DecisionScenarioEntity, String> {
    List<DecisionScenarioEntity> findAllByOrderByUpdatedAtDesc();
}
