package com.example.cardgame.llm.narrative;

import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.llm.VivoLLMClient;
import com.example.cardgame.llm.model.ChatMessage;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarrativeTextParserTest {

    @Test
    public void parse_acceptsJsonInsideMarkdownFence() {
        NarrativeTextParser parser = new NarrativeTextParser(
                new FakeVivoClient(validJsonFence()),
                new NarrativePromptBuilder(),
                new NarrativeParseValidator(),
                new FallbackNarrativeDataProvider()
        );

        ParseResult result = parser.parse("测试文本");

        assertFalse(result.isFallbackUsed());
        assertEquals(2, result.getFactions().size());
        assertEquals(1, result.getTotalNodes());
    }

    @Test
    public void parse_returnsFallbackWhenClientFails() {
        NarrativeTextParser parser = new NarrativeTextParser(
                new FailingVivoClient(),
                new NarrativePromptBuilder(),
                new NarrativeParseValidator(),
                new FallbackNarrativeDataProvider()
        );

        ParseResult result = parser.parse("测试文本");

        assertTrue(result.isFallbackUsed());
        assertEquals(4, result.getFactions().size());
    }

    private String validJsonFence() {
        return "```json\n"
                + "{"
                + "\"factions\":["
                + "{\"id\":\"a\",\"name\":\"甲方\",\"description\":\"甲方\"},"
                + "{\"id\":\"b\",\"name\":\"乙方\",\"description\":\"乙方\"}],"
                + "\"cards\":[{\"id\":\"c1\",\"factionId\":\"a\",\"title\":\"起事\",\"summary\":\"甲方起事\",\"sourceHint\":\"第1段\"}],"
                + "\"nodes\":[{\"nodeIndex\":0,\"stageTitle\":\"开始\",\"stageHint\":\"局势开始\","
                + "\"sourceHint\":\"第1段\",\"openingNarration\":\"开场\",\"resultNarration\":\"结果\","
                + "\"factionCardIds\":{\"a\":[\"c1\"],\"b\":[]}}],"
                + "\"totalNodes\":1,"
                + "\"fallbackUsed\":false"
                + "}\n```";
    }

    private static class FakeVivoClient extends VivoLLMClient {
        private final String content;

        FakeVivoClient(String content) {
            this.content = content;
        }

        @Override
        public String chat(List<ChatMessage> messages) {
            return content;
        }

        @Override
        public String chat(List<ChatMessage> messages, double temperature) {
            return content;
        }
    }

    private static class FailingVivoClient extends VivoLLMClient {
        @Override
        public String chat(List<ChatMessage> messages) throws IOException {
            throw new IOException("boom");
        }

        @Override
        public String chat(List<ChatMessage> messages, double temperature) throws IOException {
            throw new IOException("boom");
        }
    }
}
