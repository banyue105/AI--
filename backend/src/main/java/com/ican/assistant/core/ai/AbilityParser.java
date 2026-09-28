package com.ican.assistant.core.ai;

import com.ican.assistant.modules.abilitygrowth.AbilityDtos;
import java.util.List;

import com.ican.assistant.modules.abilitygrowth.AbilityDtos.ParseResult;

/** AI boundary: implementations must return reviewable candidates and must not persist them. */
public interface AbilityParser {
    ParseResult parse(String input);

    default ParseResult parse(String input, List<AbilityDtos.SkillNode> existingNodes,
                              List<AbilityDtos.SkillRelation> existingRelations) {
        return parse(input);
    }
}
