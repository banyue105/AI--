package com.ican.assistant.modules.decisionsandbox;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DecisionDtos {
    private DecisionDtos() {}

    public record ScenarioInput(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 500) String goal,
            @Min(1) @Max(3650) int timeLimitDays,
            @NotNull @DecimalMin("0.0") BigDecimal budgetYuan,
            @Min(1) @Max(1000) int peopleCount,
            boolean hasServer,
            @Size(max = 500) String changeRequest,
            @Valid List<ResourceInput> resources,
            @Valid List<RelationInput> relations) {
        public ScenarioInput {
            resources = resources == null ? List.of() : resources;
            relations = relations == null ? List.of() : relations;
            changeRequest = changeRequest == null ? "" : changeRequest.trim();
        }
    }

    public record ResourceInput(String id, @NotBlank @Size(max = 120) String label,
                                @NotBlank String type, @Min(0) Integer quantity,
                                @Size(max = 32) String unit) {}

    public record RelationInput(String id, @NotBlank String from, @NotBlank String to,
                                @NotBlank @Size(max = 120) String label,
                                @NotNull @DecimalMin("0.0") BigDecimal confidence,
                                @Size(max = 500) String assumption) {}

    public record Constraint(String id, String type, String label, String value,
                             String source, double confidence, String assumption) {}

    public record Resource(String id, String type, String label, Integer quantity, String unit,
                           String source, double confidence, String assumption) {}

    public record Node(String id, String type, String label, String source,
                       double confidence, String assumption) {}

    public record Relation(String id, String from, String to, String label,
                           String source, double confidence, String assumption) {}

    public record Range(int min, int max, String unit) {}

    public record SimulationResult(String id, String scenarioId, String optionKey, String optionName,
                                   Range timeRange, Range budgetRange, Range peopleRange,
                                   List<String> resources, String riskLevel, List<String> assumptions,
                                   List<String> impacts, String source, String explanation,
                                   String explanationSource) {}

    public record Scenario(String id, String title, String goal, int timeLimitDays,
                           BigDecimal budgetYuan, int peopleCount, boolean hasServer,
                           String changeRequest, List<Constraint> constraints,
                           List<Resource> resources, List<Node> nodes, List<Relation> relations,
                           List<SimulationResult> latestResults, int versionCount,
                           int revision, Instant createdAt, Instant updatedAt) {}

    public record Summary(String id, String title, String goal, Instant updatedAt, int versionCount) {}

    public record Suggestion(String kind, String label, String value,
                             double confidence, String assumption, String source) {}

    public record ParseRequest(@NotBlank @Size(max = 1000) String input) {}

    public record ParseResult(String summary, List<Suggestion> candidates,
                              List<String> assumptions, String source) {}

    public record Version(String id, int number, int scenarioRevision, Instant createdAt,
                          String changeSummary, Scenario snapshot, List<SimulationResult> results) {}

    public record CompareRequest(@NotBlank String leftVersionId, @NotBlank String rightVersionId) {}

    public record Comparison(String leftVersionId, String rightVersionId, String summary,
                             List<String> changedInputs, int timeMinDelta, int timeMaxDelta,
                             int budgetMinDelta, int budgetMaxDelta, String riskChange) {}

    public record RestoreRequest(@NotBlank String versionId) {}
}
