package com.ican.assistant.modules.practicereview;

import static com.ican.assistant.modules.practicereview.PracticeDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/practice")
public class PracticeController {
    private final PracticeService service;
    public PracticeController(PracticeService service) { this.service = service; }

    @GetMapping("/templates") public List<Template> templates() { return PracticeTemplates.all(); }
    @GetMapping("/projects") public List<ProjectSummary> list() { return service.list(); }
    @PostMapping("/projects") @ResponseStatus(HttpStatus.CREATED)
    public Project create(@Valid @RequestBody CreateRequest request) { return service.create(request); }
    @GetMapping("/projects/{id}") public Project get(@PathVariable String id) { return service.get(id); }
    @PutMapping("/projects/{id}")
    public Project update(@PathVariable String id, @Valid @RequestBody UpdateRequest request) { return service.update(id, request); }
    @PostMapping("/projects/{id}/criteria-suggestions")
    public Suggestions suggest(@PathVariable String id, @Valid @RequestBody RevisionRequest request) { return service.suggest(id, request.inputRevision()); }
    @PutMapping("/projects/{id}/criteria")
    public Project criteria(@PathVariable String id, @Valid @RequestBody CriteriaRequest request) { return service.saveCriteria(id, request); }
    @PostMapping("/projects/{id}/evidence") @ResponseStatus(HttpStatus.CREATED)
    public Project evidence(@PathVariable String id, @Valid @RequestBody EvidenceRequest request) { return service.saveEvidence(id, null, request); }
    @PutMapping("/projects/{id}/evidence/{evidenceId}")
    public Project evidence(@PathVariable String id, @PathVariable String evidenceId, @Valid @RequestBody EvidenceRequest request) { return service.saveEvidence(id, evidenceId, request); }
    @DeleteMapping("/projects/{id}/evidence/{evidenceId}")
    public Project removeEvidence(@PathVariable String id, @PathVariable String evidenceId, @RequestParam int inputRevision) { return service.removeEvidence(id, evidenceId, inputRevision); }
    @PostMapping("/projects/{id}/reviews") @ResponseStatus(HttpStatus.CREATED)
    public Review review(@PathVariable String id, @Valid @RequestBody ReviewRequest request) { return service.review(id, request); }
    @GetMapping("/projects/{id}/reviews") public List<ReviewSummary> history(@PathVariable String id) { return service.history(id); }
    @GetMapping("/projects/{id}/reviews/{reviewId}")
    public Review getReview(@PathVariable String id, @PathVariable String reviewId) { return service.getReview(id, reviewId); }
    @PostMapping("/projects/{id}/reviews/{reviewId}/confirm")
    public Review confirm(@PathVariable String id, @PathVariable String reviewId, @Valid @RequestBody ConfirmRequest request) { return service.confirm(id, reviewId, request); }
    @GetMapping("/projects/{id}/feedback") public List<FeedbackRecord> feedback(@PathVariable String id) { return service.feedback(id); }
    @PostMapping("/projects/{id}/feedback") @ResponseStatus(HttpStatus.CREATED)
    public List<FeedbackRecord> feedback(@PathVariable String id, @Valid @RequestBody FeedbackRequest request) { return service.saveFeedback(id, request); }
    @PostMapping("/projects/{id}/decision-origin")
    public Project origin(@PathVariable String id, @Valid @RequestBody OriginRequest request) { return service.linkDecision(id, request); }
}
