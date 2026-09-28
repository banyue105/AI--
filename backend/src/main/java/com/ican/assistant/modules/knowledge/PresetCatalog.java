package com.ican.assistant.modules.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Immutable, server-owned catalog used by both the catalog UI and AI adapters. */
@Component
public class PresetCatalog {
    private final List<KnowledgeTrack> tracks;

    public PresetCatalog(ObjectMapper objectMapper) throws IOException {
        try (var input = new ClassPathResource("knowledge/catalog.json").getInputStream()) {
            tracks = List.copyOf(objectMapper.readValue(input, new TypeReference<>() {}));
        }
    }

    public List<KnowledgeTrack> tracks() { return tracks; }

    public Optional<KnowledgeTrack> find(String id) {
        return tracks.stream().filter(track -> track.id().equals(id)).findFirst();
    }

    public List<String> ids() { return tracks.stream().map(KnowledgeTrack::id).toList(); }

    public Optional<KnowledgeTrack> findByLabel(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.strip();
        return tracks.stream().filter(track -> track.id().equalsIgnoreCase(normalized)
                || track.title().equalsIgnoreCase(normalized)
                || track.shortTitle().equalsIgnoreCase(normalized)).findFirst();
    }

    public Optional<KnowledgeTrack> selectIfMatched(String statement) {
        String text = statement == null ? "" : statement.toLowerCase(Locale.ROOT);
        if (text.matches(".*(前端|vue|react|web|页面|浏览器|typescript|javascript|css|html).*")) return find("frontend");
        if (text.matches(".*(后端|java|spring|服务端|接口|mysql|redis|mybatis).*")) return find("backend");
        if (text.matches(".*(网络|运维|linux|路由|交换|dns|tcp|部署|shell).*")) return find("network");
        if (text.matches(".*(数据分析|数据分析师|统计|sql|商业智能|bi|数据看板).*")) return find("data-analyst");
        if (text.matches(".*(机器学习|深度学习|人工智能|算法工程|模型训练|pytorch).*")) return find("ml-engineer");
        if (text.matches(".*(devops|持续集成|持续部署|ci/?cd|kubernetes|k8s|云原生).*")) return find("devops");
        if (text.matches(".*(移动端|移动开发|安卓|android|ios|flutter|手机应用|app开发).*")) return find("mobile");
        return Optional.empty();
    }

    public KnowledgeTrack select(String statement) {
        String text = statement == null ? "" : statement.toLowerCase(Locale.ROOT);
        if (text.matches(".*(前端|vue|react|web|页面|浏览器|typescript|javascript|css|html).*")) return find("frontend").orElseThrow();
        if (text.matches(".*(网络|运维|linux|路由|交换|dns|tcp|部署|shell).*")) return find("network").orElseThrow();
        if (text.matches(".*(数据分析|数据分析师|统计|sql|商业智能|bi|数据看板).*")) return find("data-analyst").orElseThrow();
        if (text.matches(".*(机器学习|深度学习|人工智能|算法工程|模型训练|pytorch).*")) return find("ml-engineer").orElseThrow();
        if (text.matches(".*(devops|持续集成|持续部署|ci/?cd|kubernetes|k8s|云原生).*")) return find("devops").orElseThrow();
        if (text.matches(".*(移动端|移动开发|安卓|android|ios|flutter|手机应用|app 开发).*")) return find("mobile").orElseThrow();
        return find("backend").orElseThrow();
    }
}
