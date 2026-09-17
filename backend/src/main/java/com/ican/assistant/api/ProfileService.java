package com.ican.assistant.api;

import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {
    public record UserProfile(String id, String name, String avatar, List<String> goals) {}
    public record UpdateProfile(
            @Pattern(regexp = "demo-user") String id,
            @NotBlank @Size(max = 80) String name,
            @Size(max = 500) String avatar,
            @NotNull @Size(min = 1, max = 10) List<@NotBlank @Size(max = 300) String> goals) {}

    private final ProfileMapper mapper;

    public ProfileService(ProfileMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public UserProfile get() {
        var row = mapper.find();
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "演示用户不存在，请检查数据库迁移");
        return new UserProfile(row.id(), row.name(), row.avatar(), mapper.goals());
    }

    @Transactional
    public UserProfile update(UpdateProfile request) {
        if (mapper.lock() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        var goals = request.goals().stream().map(String::strip).distinct().toList();
        mapper.update(request.name().strip(), request.avatar(), goals.getFirst());
        mapper.deleteGoals();
        for (int index = 0; index < goals.size(); index++) mapper.insertGoal(index, goals.get(index));
        return get();
    }
}
