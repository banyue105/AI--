package com.ican.assistant.modules.decisionsandbox;

import com.ican.assistant.modules.decisionsandbox.DecisionDtos.CompareRequest;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ParseRequest;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.RestoreRequest;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ScenarioInput;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/decisions")
public class DecisionController {
    private final DecisionScenarioService service;

    public DecisionController(DecisionScenarioService service) { this.service = service; }

    @GetMapping
    public Object list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object create(@Valid @RequestBody ScenarioInput input) { return service.create(input); }

    @GetMapping("/{id}")
    public Object get(@PathVariable String id) { return service.get(id); }

    @PutMapping("/{id}")
    public Object update(@PathVariable String id, @Valid @RequestBody ScenarioInput input) { return service.update(id, input); }

    @PostMapping("/{id}/parse")
    public Object parse(@PathVariable String id, @Valid @RequestBody ParseRequest request) {
        return service.parse(id, request.input());
    }

    @PostMapping("/{id}/simulate")
    @ResponseStatus(HttpStatus.CREATED)
    public Object simulate(@PathVariable String id) { return service.simulate(id); }

    @GetMapping("/{id}/versions")
    public Object versions(@PathVariable String id) { return service.history(id); }

    @PostMapping("/{id}/compare")
    public Object compare(@PathVariable String id, @Valid @RequestBody CompareRequest request) {
        return service.compare(id, request);
    }

    @PostMapping("/{id}/restore")
    public Object restore(@PathVariable String id, @Valid @RequestBody RestoreRequest request) {
        return service.restore(id, request.versionId());
    }
}
