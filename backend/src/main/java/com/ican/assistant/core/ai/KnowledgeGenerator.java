package com.ican.assistant.core.ai;

import com.ican.assistant.modules.knowledge.GenerateKnowledgeRequest;
import com.ican.assistant.modules.knowledge.KnowledgeTrack;

/** Replace this adapter when a real model provider is configured. */
public interface KnowledgeGenerator {
    KnowledgeTrack generate(GenerateKnowledgeRequest request);
}
