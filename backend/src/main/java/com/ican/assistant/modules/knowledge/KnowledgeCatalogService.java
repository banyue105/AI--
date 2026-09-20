package com.ican.assistant.modules.knowledge;

import com.ican.assistant.core.auth.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.assistant.core.ai.KnowledgeGenerator;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeCatalogService {
    private final KnowledgeTrackMapper mapper;
    private final KnowledgeGenerator generator;
    private final ObjectMapper objectMapper;
    private final PresetCatalog presetCatalog;
    private final CurrentUser currentUser;

    public KnowledgeCatalogService(KnowledgeTrackMapper mapper, KnowledgeGenerator generator,
                                   ObjectMapper objectMapper, PresetCatalog presetCatalog, CurrentUser currentUser) {
        this.mapper = mapper;
        this.generator = generator;
        this.objectMapper = objectMapper;
        this.currentUser = currentUser;
        this.presetCatalog = presetCatalog;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeTrack> getCatalog() {
        var tracks = new LinkedHashMap<String, KnowledgeTrack>();
        presetCatalog.tracks().forEach(track -> tracks.put(track.id(), track));
        // Older AI versions persisted generated-* tracks. Keep rows for audit, but never show
        // them as preset directions after the catalog was made canonical.
        mapper.findAllJson(currentUser.id()).stream().map(this::readTrack)
                .filter(track -> presetCatalog.find(track.id()).isPresent())
                .forEach(track -> tracks.put(track.id(), track));
        return List.copyOf(tracks.values());
    }

    @Transactional
    public KnowledgeTrack generate(GenerateKnowledgeRequest request) {
        // Generators can only select an existing server-owned preset; do not create duplicates.
        return generator.generate(request);
    }

    private String storageId(String trackId) {
        return CurrentUser.DEMO_USER_ID.equals(currentUser.id()) ? trackId : currentUser.id() + ":" + trackId;
    }

    private KnowledgeTrack readTrack(String json) {
        try {
            return objectMapper.readValue(json, KnowledgeTrack.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot read stored knowledge track", exception);
        }
    }
}
