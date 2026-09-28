package com.ican.assistant.modules.careerplanning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class CareerDtos {
    private CareerDtos() {}

    public record Job(String id, String title, String company, String category, String city,
                      String jobType, String salary, String description, String requirements,
                      List<String> skillTags) {}
    public record Preference(String category, String city, String targetJobId) {}
    public record PreferenceInput(@Size(max = 80) String category, @Size(max = 80) String city) {}
    public record TargetInput(@NotBlank String jobId) {}
    public record Match(String jobId, int score, List<String> matched, List<String> gaps) {}
    public record JobMatch(Job job, Match match) {}
    public record ReportInput(@Size(max = 64) String jobId,
                              @Size(max = 160) String jobTitle,
                              @NotBlank @Size(min = 20, max = 10000) String jdText) {}
    public record Report(String id, String jobId, String jobTitle, String jdText, int score,
                         List<String> matched, List<String> gaps, List<String> actions,
                         List<String> recognizedSkills, Instant createdAt) {}
}
