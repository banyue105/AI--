package com.ican.assistant.api;

import com.ican.assistant.core.ai.AiProperties;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class WorkspaceController {
    public record HomeData(ProfileService.UserProfile profile, String currentGoal, String recentActivity,
                           List<ModuleRegistry.ModuleManifest> modules) {}

    private final ProfileService profiles;
    private final ModuleRegistry registry;
    private final ProfileMapper mapper;
    private final AiProperties aiProperties;

    public WorkspaceController(ProfileService profiles, ModuleRegistry registry, ProfileMapper mapper, AiProperties aiProperties) {
        this.profiles = profiles;
        this.registry = registry;
        this.mapper = mapper;
        this.aiProperties = aiProperties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        mapper.ping();
        return Map.of("status", "UP", "database", "UP", "aiProvider", aiProperties.getProvider());
    }

    @GetMapping("/profile")
    public ProfileService.UserProfile profile() { return profiles.get(); }

    @PutMapping("/profile")
    public ProfileService.UserProfile updateProfile(@Valid @RequestBody ProfileService.UpdateProfile request) {
        return profiles.update(request);
    }

    @GetMapping("/modules")
    public List<ModuleRegistry.ModuleManifest> modules() { return registry.modules(); }

    @GetMapping("/home")
    public HomeData home() {
        var profile = profiles.get();
        return new HomeData(profile, profile.goals().getFirst(), "已载入数据库中保存的成长资料", registry.modules());
    }
}
