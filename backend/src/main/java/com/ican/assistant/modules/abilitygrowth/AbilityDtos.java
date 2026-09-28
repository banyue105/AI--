package com.ican.assistant.modules.abilitygrowth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class AbilityDtos {
    private AbilityDtos() {}

    public record SkillNode(
            @NotBlank @Size(max = 64) String id,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 2000) String description,
            @NotNull @Min(0) @Max(4) Integer level,
            @NotBlank @Pattern(regexp = "mastered|developing|gap|target") String status,
            @NotNull @Size(max = 100) List<@NotBlank @Size(max = 64) String> evidenceIds,
            @NotNull @DecimalMin("-100000") @DecimalMax("100000") Double x,
            @NotNull @DecimalMin("-100000") @DecimalMax("100000") Double y) {}

    public record SkillRelation(
            @NotBlank @Size(max = 64) String from,
            @NotBlank @Size(max = 64) String to,
            @NotBlank @Pattern(regexp = "prerequisite|related") String type,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double confidence) {}

    public record Evidence(String id, String title, String note, LocalDate createdAt) {}
    public record Goal(String title, LocalDate deadline) {}
    public record AbilityGraph(List<SkillNode> nodes, List<SkillRelation> relations,
                               List<Evidence> evidence, String updatedAt, String source, Goal goal) {}
    public record ParseRequest(@NotBlank @Size(max = 10000) String input,
                               @Size(max = 1000) List<@Valid SkillNode> existingNodes,
                               @Size(max = 5000) List<@Valid SkillRelation> existingRelations) {
        public ParseRequest(String input) { this(input, List.of(), List.of()); }
    }
    public record ParseResult(String summary, List<SkillNode> suggestedNodes,
                              List<SkillRelation> suggestedRelations, List<String> assumptions) {}
    public record PathRequest(
            @NotNull @Size(max = 1000) List<@NotNull @Valid SkillNode> nodes,
            @NotNull @Size(max = 5000) List<@NotNull @Valid SkillRelation> relations) {}
    public record GrowthPathStep(String skillId, String reason, List<String> prerequisiteIds, String status) {}
    public record EvidenceRequest(
            @Size(max = 64) String id,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String note,
            @PastOrPresent LocalDate createdAt) {}
}
