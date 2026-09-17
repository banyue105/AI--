package com.ican.assistant.core.ai;

import com.ican.assistant.modules.abilitygrowth.AbilityDtos.ParseResult;

/** AI boundary: implementations must return reviewable candidates and must not persist them. */
public interface AbilityParser {
    ParseResult parse(String input);
}
