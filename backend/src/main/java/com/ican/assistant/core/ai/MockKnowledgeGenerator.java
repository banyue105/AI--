package com.ican.assistant.core.ai;

import com.ican.assistant.modules.knowledge.GenerateKnowledgeRequest;
import com.ican.assistant.modules.knowledge.KnowledgeTrack;
import com.ican.assistant.modules.knowledge.PresetCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Deterministic local templates; does not call or impersonate an AI provider. */
@Component
public class MockKnowledgeGenerator implements KnowledgeGenerator {
    private final AiTextClient ai;
    private final ObjectMapper objectMapper;
    private final PresetCatalog catalog;

    public MockKnowledgeGenerator() { this(null, null, null); }

    @Autowired
    public MockKnowledgeGenerator(AiTextClient ai, ObjectMapper objectMapper, PresetCatalog catalog) {
        this.ai = ai;
        this.objectMapper = objectMapper;
        this.catalog = catalog;
    }

    @Override
    public KnowledgeTrack generate(GenerateKnowledgeRequest request) {
        if (catalog == null) throw new IllegalStateException("Preset catalog is unavailable");
        String query = request.query() == null ? "" : request.query().strip();
        if (query.codePointCount(0, query.length()) < 2 || query.matches("[\\d\\s]+")) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "未查询到有效目标，请输入具体技术栈或成长方向");
        }
        boolean aiAvailable = ai != null && objectMapper != null && ai.enabled();
        String aiId = aiAvailable ? tryAi(request) : null;
        if (aiId != null) return catalog.find(aiId).orElseThrow();
        var matched = catalog.selectIfMatched(request.query());
        if (matched.isPresent()) return matched.get();
        throw new ResponseStatusException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                aiAvailable ? "AI 无法从预置方向中匹配该目标，请输入更具体的描述" : "无法匹配预置方向，请从预置方向中选择");
    }

    private String tryAi(GenerateKnowledgeRequest request) {
        if (ai == null || objectMapper == null || !ai.enabled()) return null;
        try {
            List<String> allowedIds = catalog.ids();
            String allowed = catalog.tracks().stream()
                    .map(track -> track.id() + "=" + track.title() + "（" + track.shortTitle() + "）")
                    .collect(Collectors.joining("、"));
            var response = ai.completeJson(
                    "TASK=TECH_STACK_SELECTION。只返回 JSON。根据用户描述从允许的预置方向中选择一个，字段 directionId、reason。只有确实相关时才选择；如果没有匹配方向，directionId 必须为 null，reason 说明无法匹配。不得创建新方向、阶段或技能。允许方向：" + allowed,
                    objectMapper.writeValueAsString(Map.of("request", request, "allowedDirectionIds", allowedIds)));
            if (response.isEmpty()) return null;
            String id = response.get().path("directionId").asText("");
            String label = response.get().path("direction").asText("");
            return catalog.findByLabel(id).or(() -> catalog.findByLabel(label)).map(KnowledgeTrack::id).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
