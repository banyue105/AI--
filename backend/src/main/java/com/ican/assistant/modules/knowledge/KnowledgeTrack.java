package com.ican.assistant.modules.knowledge;

import java.util.List;

/** API shape shared with the frontend knowledge catalog. */
public record KnowledgeTrack(
        String id,
        String title,
        String shortTitle,
        String description,
        String outcome,
        String source,
        List<Stage> stages) {

    public record Stage(String id, String title, String description, List<Item> items) {}

    public record Item(String id, String name, String description, List<String> aliases, String priority) {}
}
