package com.ican.assistant.modules.knowledge;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    private final KnowledgeCatalogService service;

    public KnowledgeController(KnowledgeCatalogService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    public List<KnowledgeTrack> getCatalog() {
        return service.getCatalog();
    }

    @PostMapping("/generate")
    public KnowledgeTrack generate(@Valid @RequestBody GenerateKnowledgeRequest request) {
        return service.generate(request);
    }
}
