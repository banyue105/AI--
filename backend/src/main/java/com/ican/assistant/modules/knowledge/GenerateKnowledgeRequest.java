package com.ican.assistant.modules.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GenerateKnowledgeRequest(
        @NotBlank @Size(max = 120) String query,
        @NotNull @Size(max = 200) List<@NotNull @Valid CurrentSkill> currentSkills) {

    public record CurrentSkill(
            @NotBlank @Size(max = 120) String name,
            @NotNull @Min(0) @Max(4) Integer level,
            @NotBlank @Pattern(regexp = "mastered|developing|gap|target") String status) {}
}
