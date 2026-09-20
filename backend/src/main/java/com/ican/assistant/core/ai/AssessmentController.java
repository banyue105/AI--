package com.ican.assistant.core.ai;

import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ability/skills")
public class AssessmentController {
    private final AiAnalysisService service;

    public AssessmentController(AiAnalysisService service) { this.service = service; }

    @GetMapping("/{id}/assessment/questions")
    public AiAnalysisService.AssessmentResponse questions(@PathVariable @Size(max = 64) String id) {
        return service.questions(id);
    }
}
