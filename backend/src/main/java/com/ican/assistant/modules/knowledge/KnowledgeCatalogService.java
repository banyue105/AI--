package com.ican.assistant.modules.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.assistant.core.ai.KnowledgeGenerator;
import java.io.IOException;
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
    private final List<KnowledgeTrack> presetCatalog;

    public KnowledgeCatalogService(KnowledgeTrackMapper mapper, KnowledgeGenerator generator,
                                   ObjectMapper objectMapper) throws IOException {
        this.mapper = mapper;
        this.generator = generator;
        this.objectMapper = objectMapper;
        try (var input = new ClassPathResource("knowledge/catalog.json").getInputStream()) {
            this.presetCatalog = objectMapper.readValue(input, new TypeReference<>() {});
        }
    }

    @Transactional(readOnly = true)
    public List<KnowledgeTrack> getCatalog() {
        var tracks = new LinkedHashMap<String, KnowledgeTrack>();
        presetCatalog.forEach(track -> tracks.put(track.id(), track));
        mapper.findAllJson().stream().map(this::readTrack).forEach(track -> tracks.put(track.id(), track));
        return List.copyOf(tracks.values());
    }

    @Transactional
    public KnowledgeTrack generate(GenerateKnowledgeRequest request) {
        KnowledgeTrack track = generator.generate(request);
        try {
            mapper.save(track.id(), request.query().strip(), objectMapper.writeValueAsString(track));
            return track;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize generated knowledge track", exception);
        }
    }

    private KnowledgeTrack readTrack(String json) {
        try {
            return objectMapper.readValue(json, KnowledgeTrack.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot read stored knowledge track", exception);
        }
    }
}
