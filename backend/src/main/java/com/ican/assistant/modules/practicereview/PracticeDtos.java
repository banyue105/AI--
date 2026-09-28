package com.ican.assistant.modules.practicereview;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PracticeDtos {
    private PracticeDtos() {}

    public record Criterion(String id, String title, String standard, String expectedEvidence,
                            boolean required, int order) {}
    public record Template(String id, String title, String description, List<Criterion> criteria) {}
    public record Evidence(String id, int revision, String title, String kind, String content,
                           List<String> criterionIds, Instant updatedAt) {}
    public record Citation(String evidenceId, int evidenceRevision, int startLine, int endLine, String quote) {}
    public record Finding(String criterionId, String verdict, String reason,
                          List<Citation> citations, String nextAction) {}
    public record Expected(BigDecimal min, BigDecimal max) {}
    public record InputMetric(@NotBlank String metric, @NotBlank String unit, @NotBlank String scope,
                              @NotBlank String basis, @Valid Expected expected, BigDecimal actual,
                              @NotBlank String baselineTiming, Instant baselineRecordedAt) {}
    public record MetricComparison(String metric, String unit, String scope, String basis,
                                   Expected expected, BigDecimal actual, boolean comparable, String reason,
                                   String position, BigDecimal deltaFromMin, BigDecimal deltaFromMax,
                                   BigDecimal percentDelta, String baselineTiming) {}
    public record SourceRange(int min, int max, String unit) {}
    public record SourceSnapshot(String title, String goal, String optionName, SourceRange timeRange,
                                 SourceRange budgetRange, List<String> assumptions) {}
    public record DecisionOrigin(String scenarioId, String versionId, String optionKey,
                                 Instant capturedAt, SourceSnapshot snapshot) {}
    public record Project(String id, String title, String goal, String templateId, int inputRevision,
                          List<Criterion> criteria, List<Evidence> evidence, List<InputMetric> metrics,
                          String processNote, DecisionOrigin decisionOrigin, Instant createdAt,
                          Instant updatedAt, List<MetricComparison> comparisons) {}
    public record ProjectSummary(String id, String title, String goal, int inputRevision,
                                 int evidenceCount, int reviewCount, Instant updatedAt) {}
    public record Override(@NotBlank String criterionId, @NotBlank String verdict,
                           @NotBlank String reason, @NotNull List<Citation> citations) {}
    public record Confirmation(Instant confirmedAt, List<Override> overrides) {}
    public record Review(String id, String projectId, int number, int inputRevision, Project input,
                         List<Finding> findings, String source, String providerNotice, Instant createdAt,
                         Confirmation confirmation, boolean isStale) {}
    public record ReviewSummary(String id, int number, int inputRevision, String source,
                                Instant createdAt, Instant confirmedAt, boolean isStale) {}
    public record FeedbackRecord(String id, String projectId, String reviewId, String target,
                                 String targetId, String targetName, String title, String note, List<String> criterionIds,
                                 List<String> evidenceIds, String delivery, String externalRecordId,
                                 Instant confirmedAt) {}
    public record Suggestion(String title, String standard, String expectedEvidence, boolean required) {}
    public record Suggestions(List<Suggestion> candidates, List<String> assumptions,
                              String source, String providerNotice) {}
    public record Analysis(List<Finding> findings, String source, String providerNotice) {}

    public record CreateRequest(@NotBlank String title, @NotBlank String goal, @NotBlank String templateId) {}
    public record UpdateRequest(@Min(1) int inputRevision, @NotBlank String title, @NotBlank String goal,
                                @NotNull @Size(max = 12) @Valid List<InputMetric> metrics, String processNote) {}
    public record RevisionRequest(@Min(1) int inputRevision) {}
    public record CriterionInput(String id, @NotBlank String title, @NotBlank String standard,
                                 @NotBlank String expectedEvidence, boolean required) {}
    public record CriteriaRequest(@Min(1) int inputRevision,
                                  @NotNull @Size(min = 1, max = 12) @Valid List<CriterionInput> criteria) {}
    public record EvidenceRequest(@Min(1) int inputRevision, @NotBlank String title,
                                  @NotBlank String kind, @NotBlank String content,
                                  @NotNull @Size(max = 12) List<String> criterionIds) {}
    public record ReviewRequest(@Min(1) int inputRevision, @NotBlank String requestId) {}
    public record ConfirmRequest(@Min(1) int inputRevision,
                                 @NotNull @Size(max = 12) @Valid List<Override> overrides) {}
    public record FeedbackItem(@NotBlank String target, String targetId, String targetName, @NotBlank String title,
                               @NotBlank String note, @NotNull @Size(max = 12) List<String> criterionIds,
                               @NotNull @Size(max = 10) List<String> evidenceIds) {}
    public record FeedbackRequest(@NotBlank String reviewId, @NotBlank String requestId,
                                  @NotNull @Size(min = 1, max = 12) @Valid List<FeedbackItem> items) {}
    public record OriginRequest(@Min(1) int inputRevision, @NotBlank String scenarioId,
                                @NotBlank String versionId, @NotBlank String optionKey) {}
}
