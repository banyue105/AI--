package com.ican.assistant.modules.decisionsandbox;

import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ScenarioInput;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DecisionDemoSeed implements ApplicationRunner {
    private final DecisionScenarioRepository scenarios;
    private final DecisionScenarioService service;
    private final boolean enabled;

    public DecisionDemoSeed(DecisionScenarioRepository scenarios, DecisionScenarioService service,
                            @Value("${app.seed-demo:true}") boolean enabled) {
        this.scenarios = scenarios;
        this.service = service;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || scenarios.count() > 0) return;
        var scene = service.create(new ScenarioInput("60 天完成 AI 网页项目", "在现有资源内交付可演示的 AI 网页项目",
                60, new BigDecimal("100000"), 3, true, "增加视觉识别功能", List.of(), List.of()));
        service.simulate(scene.id());
    }
}
