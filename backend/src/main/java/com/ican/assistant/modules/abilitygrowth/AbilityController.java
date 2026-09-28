package com.ican.assistant.modules.abilitygrowth;

import com.ican.assistant.core.ai.AbilityParser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.ican.assistant.modules.abilitygrowth.AbilityDtos.*;

@RestController
@RequestMapping("/api/v1/ability")
public class AbilityController {
    private final AbilityService service;
    private final GrowthPathPlanner planner;
    private final AbilityParser parser;

    public AbilityController(AbilityService service, GrowthPathPlanner planner, AbilityParser parser) {
        this.service = service;
        this.planner = planner;
        this.parser = parser;
    }

    @GetMapping("/graph")
    public AbilityGraph graph() { return service.graph(); }

    @PostMapping("/skills")
    public AbilityGraph saveSkill(@Valid @RequestBody SkillNode request) { return service.saveSkill(request); }

    @PutMapping("/skills/{id}")
    public AbilityGraph updateSkill(@PathVariable @Size(max = 64) String id, @Valid @RequestBody SkillNode request) {
        return service.updateSkill(id, request);
    }

    @DeleteMapping("/skills/{id}")
    public AbilityGraph deleteSkill(@PathVariable @Size(max = 64) String id) { return service.deleteSkill(id); }

    @PostMapping("/relations")
    public AbilityGraph saveRelation(@Valid @RequestBody SkillRelation request) { return service.saveRelation(request); }

    @PostMapping("/parse")
    public ParseResult parse(@Valid @RequestBody ParseRequest request) {
        return parser.parse(request.input(), request.existingNodes(), request.existingRelations());
    }

    @PostMapping("/path")
    public List<GrowthPathStep> path(@Valid @RequestBody PathRequest request) {
        return planner.plan(request.nodes(), request.relations());
    }

    @PostMapping("/skills/{id}/evidence")
    public AbilityGraph addEvidence(@PathVariable @Size(max = 64) String id, @Valid @RequestBody EvidenceRequest request) {
        return service.addEvidence(id, request);
    }
}
