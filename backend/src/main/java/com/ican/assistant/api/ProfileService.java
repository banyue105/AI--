package com.ican.assistant.api;

import com.ican.assistant.core.auth.CurrentUser;
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
            @Size(max = 64) String id,
            @NotBlank @Size(max = 80) String name,
            @Size(max = 500) String avatar,
            @NotNull @Size(min = 1, max = 10) List<@NotBlank @Size(max = 300) String> goals) {}

    private final ProfileMapper mapper;
    private final CurrentUser currentUser;

    public ProfileService(ProfileMapper mapper, CurrentUser currentUser) {
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    private String userId() { return currentUser.id(); }

    @Transactional(readOnly = true)
    public UserProfile get() {
        var userId = userId();
        var row = mapper.find(userId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "当前用户不存在，请先完成注册或登录");
        return new UserProfile(row.id(), row.name(), row.avatar(), mapper.goals(userId));
    }

    @Transactional
    public UserProfile update(UpdateProfile request) {
        var userId = userId();
        if (mapper.lock(userId) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        if (request.id() != null && !request.id().isBlank() && !userId.equals(request.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能修改其他用户资料");
        }
        var goals = request.goals().stream().map(String::strip).distinct().toList();
        mapper.update(userId, request.name().strip(), request.avatar(), goals.getFirst());
        mapper.deleteGoals(userId);
        for (int index = 0; index < goals.size(); index++) mapper.insertGoal(userId, index, goals.get(index));
        return get();
    }
}
