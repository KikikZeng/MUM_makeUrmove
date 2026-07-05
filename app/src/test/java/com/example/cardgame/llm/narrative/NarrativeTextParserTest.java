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
                new FallbackNarrativeDataProvider(),
                new NarrativePreChecker()
        );

        ParseResult result = parser.parse(validInputText());

        assertFalse(result.isFallbackUsed());
        assertEquals(ParseResult.STATUS_SUCCESS, result.getParseStatus());
        assertEquals(2, result.getFactions().size());
        assertEquals(2, result.getTotalNodes());
    }

    @Test
    public void parse_returnsFallbackWhenClientFails() {
        NarrativeTextParser parser = new NarrativeTextParser(
                new FailingVivoClient(),
                new NarrativePromptBuilder(),
                new NarrativeParseValidator(),
                new FallbackNarrativeDataProvider(),
                new NarrativePreChecker()
        );

        ParseResult result = parser.parse(validInputText());

        assertTrue(result.isFallbackUsed());
        assertEquals(ParseResult.STATUS_PARSE_ERROR, result.getParseStatus());
        assertEquals(4, result.getFactions().size());
    }

    @Test
    public void parse_returnsFactionInvalidWithoutCallingLlmWhenPrecheckFails() {
        CountingVivoClient client = new CountingVivoClient(validJsonFence());
        NarrativeTextParser parser = new NarrativeTextParser(
                client,
                new NarrativePromptBuilder(),
                new NarrativeParseValidator(),
                new FallbackNarrativeDataProvider(),
                new NarrativePreChecker()
        );

        ParseResult result = parser.parse("太短");

        assertEquals(0, client.callCount);
        assertFalse(result.isFallbackUsed());
        assertEquals(ParseResult.STATUS_FACTION_COUNT_INVALID, result.getParseStatus());
        assertTrue(result.isRequiresTextEdit());
    }

    @Test
    public void parse_retriesParseErrorOnceAndReturnsSuccessWhenRetryWorks() {
        SequenceVivoClient client = new SequenceVivoClient("不是JSON", validJsonFence());
        NarrativeTextParser parser = new NarrativeTextParser(
                client,
                new NarrativePromptBuilder(),
                new NarrativeParseValidator(),
                new FallbackNarrativeDataProvider(),
                new NarrativePreChecker()
        );

        ParseResult result = parser.parse(validInputText());

        assertEquals(2, client.callCount);
        assertEquals(ParseResult.STATUS_SUCCESS, result.getParseStatus());
        assertFalse(result.isFallbackUsed());
    }

    @Test
    public void parse_returnsMissingActionWhenOneFactionHasNoCards() {
        NarrativeTextParser parser = new NarrativeTextParser(
                new FakeVivoClient(missingActionJson()),
                new NarrativePromptBuilder(),
                new NarrativeParseValidator(),
                new FallbackNarrativeDataProvider(),
                new NarrativePreChecker()
        );

        ParseResult result = parser.parse(validInputText());

        assertEquals(ParseResult.STATUS_MISSING_ACTION, result.getParseStatus());
        assertTrue(result.isRequiresTextEdit());
        assertFalse(result.isRetryAllowed());
    }

    private String validJsonFence() {
        return "```json\n"
                + "{"
                + "\"factions\":["
                + "{\"id\":\"a\",\"name\":\"甲方\",\"description\":\"甲方\"},"
                + "{\"id\":\"b\",\"name\":\"乙方\",\"description\":\"乙方\"}],"
                + "\"cards\":["
                + "{\"id\":\"c1\",\"factionId\":\"a\",\"title\":\"起事\",\"summary\":\"甲方发动起事\",\"sourceHint\":\"第1段\"},"
                + "{\"id\":\"c2\",\"factionId\":\"b\",\"title\":\"镇压\",\"summary\":\"乙方组织镇压\",\"sourceHint\":\"第2段\"}],"
                + "\"nodes\":[{\"nodeIndex\":0,\"stageTitle\":\"开始\",\"stageHint\":\"局势开始\","
                + "\"sourceHint\":\"第1段\",\"openingNarration\":\"开场\",\"resultNarration\":\"结果\","
                + "\"factionCardIds\":{\"a\":[\"c1\"],\"b\":[]}},"
                + "{\"nodeIndex\":1,\"stageTitle\":\"镇压\",\"stageHint\":\"对方开始反制\","
                + "\"sourceHint\":\"第2段\",\"openingNarration\":\"反制\",\"resultNarration\":\"结束\","
                + "\"factionCardIds\":{\"a\":[],\"b\":[\"c2\"]}}],"
                + "\"totalNodes\":2,"
                + "\"fallbackUsed\":false"
                + "}\n```";
    }

    private String missingActionJson() {
        return "{"
                + "\"factions\":["
                + "{\"id\":\"a\",\"name\":\"甲方\",\"description\":\"甲方\"},"
                + "{\"id\":\"b\",\"name\":\"乙方\",\"description\":\"乙方\"}],"
                + "\"cards\":[{\"id\":\"c1\",\"factionId\":\"a\",\"title\":\"起事\",\"summary\":\"甲方发动起事\",\"sourceHint\":\"第1段\"}],"
                + "\"nodes\":[{\"nodeIndex\":0,\"stageTitle\":\"开始\",\"stageHint\":\"局势开始\","
                + "\"sourceHint\":\"第1段\",\"openingNarration\":\"开场\",\"resultNarration\":\"结果\","
                + "\"factionCardIds\":{\"a\":[\"c1\"],\"b\":[]}}],"
                + "\"totalNodes\":1,"
                + "\"fallbackUsed\":false"
                + "}";
    }

    private String validInputText() {
        return "1911年，革命党在武昌发动起义，清朝政府随后组织镇压，双方围绕政权展开冲突。";
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

    private static class CountingVivoClient extends FakeVivoClient {
        int callCount;

        CountingVivoClient(String content) {
            super(content);
        }

        @Override
        public String chat(List<ChatMessage> messages, double temperature) {
            callCount++;
            return super.chat(messages, temperature);
        }
    }

    private static class SequenceVivoClient extends VivoLLMClient {
        private final String first;
        private final String second;
        int callCount;

        SequenceVivoClient(String first, String second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public String chat(List<ChatMessage> messages, double temperature) {
            callCount++;
            return callCount == 1 ? first : second;
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
