package com.ican.assistant.modules.abilitygrowth;

import com.ican.assistant.core.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static com.ican.assistant.modules.abilitygrowth.AbilityDtos.*;

@Service
public class AbilityService {
    private final AbilityMapper mapper;
    private final GrowthPathPlanner planner;
    private final CurrentUser currentUser;

    public AbilityService(AbilityMapper mapper, GrowthPathPlanner planner, CurrentUser currentUser) {
        this.mapper = mapper;
        this.planner = planner;
        this.currentUser = currentUser;
    }

    private String userId() { return currentUser.id(); }

    @Transactional(readOnly = true)
    public AbilityGraph graph() {
        var userId = userId();
        var links = mapper.findEvidenceLinks(userId);
        var nodes = mapper.findSkills(userId).stream().map(row -> new SkillNode(
                row.id(), row.name(), row.description(), row.level(), row.status(),
                links.stream().filter(link -> link.skillId().equals(row.id()))
                        .map(AbilityMapper.EvidenceLink::evidenceId).toList(), row.x(), row.y())).toList();
        var relations = mapper.findRelations(userId).stream().map(row ->
                new SkillRelation(row.fromId(), row.toId(), row.type(), row.confidence())).toList();
        var evidence = mapper.findEvidence(userId).stream().map(row ->
                new Evidence(row.id(), row.title(), row.note(), row.createdAt())).toList();
        var goal = mapper.findGoal(userId);
        LocalDateTime updatedAt = mapper.findUpdatedAt(userId);
        if (goal == null || updatedAt == null) throw error(HttpStatus.NOT_FOUND, "演示用户的能力图谱不存在");
        return new AbilityGraph(nodes, relations, evidence, updatedAt.atOffset(ZoneOffset.UTC).toString(),
                "api", new Goal(goal.title(), goal.deadline()));
    }

    @Transactional
    public AbilityGraph saveSkill(SkillNode request) {
        lockGraph();
        return save(request, false);
    }

    @Transactional
    public AbilityGraph updateSkill(String id, SkillNode request) {
        if (!id.equals(request.id())) throw error(HttpStatus.BAD_REQUEST, "路径 id 必须与能力 id 一致");
        lockGraph();
        return save(request, true);
    }

    private AbilityGraph save(SkillNode request, boolean mustExist) {
        var userId = userId();
        var skills = mapper.findSkills(userId);
        var byId = skills.stream().filter(item -> item.id().equals(request.id())).findFirst();
        String normalizedName = normalize(request.name());
        var byName = skills.stream().filter(item -> normalize(item.name()).equals(normalizedName)).findFirst();
        if (mustExist && byId.isEmpty()) throw error(HttpStatus.NOT_FOUND, "能力不存在");
        if (byId.isPresent() && byName.isPresent() && !byId.get().id().equals(byName.get().id())) {
            throw error(HttpStatus.CONFLICT, "该名称已被另一项能力使用");
        }
        var existing = byId.or(() -> byName);
        Set<String> knownEvidence = new HashSet<>();
        mapper.findEvidence(userId).forEach(item -> knownEvidence.add(item.id()));
        if (!knownEvidence.containsAll(request.evidenceIds())) {
            throw error(HttpStatus.BAD_REQUEST, "存在未知的证据 id，请先添加实践证据");
        }
        String savedId = existing.map(AbilityMapper.SkillRow::id).orElse(request.id());
        double x = !mustExist && existing.isPresent() ? existing.get().x() : request.x();
        double y = !mustExist && existing.isPresent() ? existing.get().y() : request.y();
        var saved = new SkillNode(savedId, request.name().strip(), Objects.requireNonNullElse(request.description(), ""),
                request.level(), request.status(), request.evidenceIds().stream().distinct().toList(), x, y);
        if (existing.isPresent()) mapper.updateSkill(userId, saved, normalizedName);
        else {
            int sortOrder = skills.stream().mapToInt(AbilityMapper.SkillRow::sortOrder).max().orElse(-1) + 1;
            mapper.insertSkill(userId, saved, normalizedName, sortOrder);
        }
        mapper.deleteSkillEvidence(userId, savedId);
        saved.evidenceIds().forEach(evidenceId -> mapper.insertEvidenceLink(userId, savedId, evidenceId));
        touch();
        return graph();
    }

    @Transactional
    public AbilityGraph saveRelation(SkillRelation relation) {
        lockGraph();
        AbilityGraph graph = graph();
        var updatedRelations = new ArrayList<>(graph.relations());
        boolean existing = updatedRelations.removeIf(item -> item.from().equals(relation.from())
                && item.to().equals(relation.to()) && item.type().equals(relation.type()));
        updatedRelations.add(relation);
        // Validate every reference and the complete prerequisite DAG before any write.
        planner.plan(graph.nodes(), updatedRelations);
        var userId = userId();
        if (existing) mapper.updateRelation(userId, relation);
        else mapper.insertRelation(userId, relation, graph.relations().size());
        touch();
        return graph();
    }

    @Transactional
    public AbilityGraph deleteSkill(String skillId) {
        lockGraph();
        var userId = userId();
        if (mapper.findSkills(userId).stream().noneMatch(item -> item.id().equals(skillId))) {
            throw error(HttpStatus.NOT_FOUND, "能力不存在");
        }
        mapper.deleteRelationsBySkill(userId, skillId);
        mapper.deleteSkillEvidence(userId, skillId);
        mapper.deleteSkill(userId, skillId);
        touch();
        return graph();
    }

    @Transactional
    public AbilityGraph addEvidence(String skillId, EvidenceRequest request) {
        lockGraph();
        var userId = userId();
        if (mapper.findSkills(userId).stream().noneMatch(item -> item.id().equals(skillId))) {
            throw error(HttpStatus.NOT_FOUND, "能力不存在");
        }
        String id = request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id();
        if (mapper.findEvidence(userId).stream().anyMatch(item -> item.id().equals(id))) {
            throw error(HttpStatus.CONFLICT, "证据 id 已存在");
        }
        var evidence = new Evidence(id, request.title().strip(), Objects.requireNonNullElse(request.note(), ""),
                request.createdAt() == null ? LocalDate.now(ZoneOffset.UTC) : request.createdAt());
        mapper.insertEvidence(userId, evidence);
        mapper.insertEvidenceLink(userId, skillId, id);
        touch();
        return graph();
    }

    private void lockGraph() {
        if (mapper.lockGraph(userId()) == null) throw error(HttpStatus.NOT_FOUND, "当前用户的能力图谱不存在");
    }

    private void touch() {
        mapper.touch(userId(), LocalDateTime.now(ZoneOffset.UTC));
    }

    private String normalize(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
