package com.example.cardgame.llm.narrative;

import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.llm.GlmLLMClient;
import com.google.gson.Gson;

import java.io.IOException;

public class NarrativeTextParser {
    private final GlmLLMClient llmClient;
    private final NarrativePromptBuilder promptBuilder;
    private final NarrativeParseValidator parseValidator;
    private final FallbackNarrativeDataProvider fallbackDataProvider;
    private final Gson gson;

    public NarrativeTextParser() {
        this(new GlmLLMClient(), new NarrativePromptBuilder(),
                new NarrativeParseValidator(), new FallbackNarrativeDataProvider());
    }

    public NarrativeTextParser(GlmLLMClient llmClient, NarrativePromptBuilder promptBuilder,
                               NarrativeParseValidator parseValidator,
                               FallbackNarrativeDataProvider fallbackDataProvider) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parseValidator = parseValidator;
        this.fallbackDataProvider = fallbackDataProvider;
        this.gson = new Gson();
    }

    public ParseResult parse(String rawText) {
        try {
            String content = llmClient.chat(promptBuilder.buildMessages(sanitizeText(rawText)));
            ParseResult parseResult = gson.fromJson(extractJsonObject(content), ParseResult.class);
            return parseValidator.validateOrFallback(parseResult);
        } catch (Exception e) {
            return fallbackDataProvider.getFallbackData();
        }
    }

    private String sanitizeText(String rawText) throws IOException {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IOException("rawText is empty");
        }
        String cleaned = rawText.trim();
        return cleaned.length() > 5000 ? cleaned.substring(0, 5000) : cleaned;
    }

    String extractJsonObject(String content) throws IOException {
        if (content == null) {
            throw new IOException("LLM content is null");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IOException("No JSON object found");
        }
        return trimmed.substring(start, end + 1);
    }
}
