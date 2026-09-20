package com.ican.assistant.core.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiAnalysisService service;

    public AiController(AiAnalysisService service) { this.service = service; }

    @PostMapping("/ability-update")
    public AiAnalysisService.AbilityUpdateResponse updateAbility(
            @Valid @RequestBody AiAnalysisService.AbilityUpdateRequest request) {
        return service.updateAbility(request.statement());
    }

    @PostMapping("/skill-enrichment")
    public AiAnalysisService.SkillEnrichmentResponse enrichSkill(
            @Valid @RequestBody AiAnalysisService.SkillEnrichmentRequest request) {
        return service.enrichSkill(request);
    }

    @PostMapping("/tech-stack")
    public AiAnalysisService.TechStackResponse techStack(
            @Valid @RequestBody AiAnalysisService.TechStackRequest request) {
        return service.generateTechStack(request);
    }

    @PostMapping("/explainable-path")
    public AiAnalysisService.ExplainablePathResponse explainablePath(
            @Valid @RequestBody AiAnalysisService.ExplainablePathRequest request) {
        return service.explainablePath(request);
    }
}
