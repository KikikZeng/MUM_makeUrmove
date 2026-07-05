package com.example.cardgame.llm.narrative;

import android.util.Log;

import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.llm.VivoLLMClient;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class NarrativeTextParser {
    private static final String TAG = "NarrativeTextParser";
    private static final double FIRST_TEMPERATURE = 0.1;
    private static final double RETRY_TEMPERATURE = 0.0;

    private final VivoLLMClient llmClient;
    private final NarrativePromptBuilder promptBuilder;
    private final NarrativeParseValidator parseValidator;
    private final FallbackNarrativeDataProvider fallbackDataProvider;
    private final NarrativePreChecker preChecker;
    private final Gson gson;

    public NarrativeTextParser() {
        this(new VivoLLMClient(), new NarrativePromptBuilder(),
                new NarrativeParseValidator(), new FallbackNarrativeDataProvider(),
                new NarrativePreChecker());
    }

    public NarrativeTextParser(VivoLLMClient llmClient, NarrativePromptBuilder promptBuilder,
                               NarrativeParseValidator parseValidator,
                               FallbackNarrativeDataProvider fallbackDataProvider,
                               NarrativePreChecker preChecker) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parseValidator = parseValidator;
        this.fallbackDataProvider = fallbackDataProvider;
        this.preChecker = preChecker;
        this.gson = new Gson();
    }

    public ParseResult parse(String rawText) {
        Log.i(TAG, "=== 开始解析文本 ===");
        Log.i(TAG, "Raw text length: " + (rawText != null ? rawText.length() : 0));
        String textPreview = rawText != null && rawText.length() > 300 ? rawText.substring(0, 300) + "..." : rawText;
        Log.i(TAG, "Raw text preview: " + textPreview);

        try {
            String sanitized = sanitizeText(rawText);
            Log.d(TAG, "Sanitized text length: " + sanitized.length());

            NarrativePreChecker.CheckResult preCheck = preChecker.check(sanitized);
            if (!preCheck.isPassed()) {
                Log.w(TAG, "Pre-check failed: " + preCheck.getReason());
                return failureResult(ParseResult.STATUS_FACTION_COUNT_INVALID,
                        "材料不符合解析条件：" + preCheck.getReason(),
                        false,
                        true);
            }

            ParseResult result = tryParseWithRetry(sanitized);
            if (result.isFallbackUsed()) {
                Log.e(TAG, "=== 解析失败，使用 Fallback 数据 ===");
                Log.e(TAG, "Fallback data: 安史之乱预设数据");
            } else {
                Log.i(TAG, "=== 解析成功 ===");
                Log.i(TAG, "Factions count: " + (result.getFactions() != null ? result.getFactions().size() : 0));
                Log.i(TAG, "Cards count: " + (result.getCards() != null ? result.getCards().size() : 0));
                Log.i(TAG, "Nodes count: " + (result.getNodes() != null ? result.getNodes().size() : 0));
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "=== 解析失败，使用 Fallback 数据 ===");
            Log.e(TAG, "Exception: " + e.getMessage(), e);
            Log.e(TAG, "Fallback data: 安史之乱预设数据");
            return fallbackResult("模型调用失败，已使用演示兜底数据：" + e.getMessage());
        }
    }

    private ParseResult tryParseWithRetry(String sanitizedText) throws IOException {
        Log.d(TAG, "First attempt with temperature " + FIRST_TEMPERATURE);
        ParseAttempt firstAttempt = parseOnce(sanitizedText, FIRST_TEMPERATURE);

        if (firstAttempt.hasFormatError()) {
            Log.w(TAG, "First attempt format error: " + firstAttempt.errorMessage);
            return retryAfterParseError(sanitizedText);
        }

        ParseResult firstResult = firstAttempt.parseResult;
        if (isEmptyModelOutput(firstResult)) {
            return missingActionResult("文本事件不足，无法形成至少两个具备事件牌的阵营，请补充材料后重试");
        }

        String invalidReason = parseValidator.getInvalidReason(firstResult);
        if (invalidReason == null) {
            return successResult(firstResult);
        }

        Log.w(TAG, "First parse attempt invalid: " + invalidReason);
        if (!shouldRetryValidationError(invalidReason)) {
            return validationFailure(invalidReason);
        }

        sleepBeforeRetry();
        Log.d(TAG, "Retrying validation error with temperature " + RETRY_TEMPERATURE);
        ParseAttempt retryAttempt = parseOnce(sanitizedText, RETRY_TEMPERATURE);
        if (retryAttempt.hasFormatError()) {
            return parseErrorResult("解析异常，系统已自动重试但仍未通过：" + retryAttempt.errorMessage);
        }
        if (isEmptyModelOutput(retryAttempt.parseResult)) {
            return missingActionResult("文本事件不足，无法形成至少两个具备事件牌的阵营，请补充材料后重试");
        }
        String retryInvalidReason = parseValidator.getInvalidReason(retryAttempt.parseResult);
        if (retryInvalidReason != null) {
            return validationFailure(retryInvalidReason);
        }
        return successResult(retryAttempt.parseResult);
    }

    private ParseResult retryAfterParseError(String sanitizedText) throws IOException {
        sleepBeforeRetry();
        Log.d(TAG, "Retrying parse error with temperature " + RETRY_TEMPERATURE);
        ParseAttempt retryAttempt = parseOnce(sanitizedText, RETRY_TEMPERATURE);
        if (retryAttempt.hasFormatError()) {
            return parseErrorResult("解析异常，系统已自动重试但仍未通过：" + retryAttempt.errorMessage);
        }
        if (isEmptyModelOutput(retryAttempt.parseResult)) {
            return missingActionResult("文本事件不足，无法形成至少两个具备事件牌的阵营，请补充材料后重试");
        }
        String invalidReason = parseValidator.getInvalidReason(retryAttempt.parseResult);
        if (invalidReason != null) {
            return validationFailure(invalidReason);
        }
        return successResult(retryAttempt.parseResult);
    }

    private ParseAttempt parseOnce(String sanitizedText, double temperature) throws IOException {
        String content = llmClient.chat(promptBuilder.buildMessages(sanitizedText), temperature);
        Log.d(TAG, "Raw LLM response length: " + (content != null ? content.length() : 0));
        try {
            String jsonObject = extractJsonObject(content);
            jsonObject = cleanJsonPunctuation(jsonObject);
            validateRootFields(jsonObject);
            Log.d(TAG, "Extracted JSON length: " + jsonObject.length());
            return ParseAttempt.success(gson.fromJson(jsonObject, ParseResult.class));
        } catch (Exception e) {
            Log.e(TAG, "Parse attempt failed: " + e.getMessage(), e);
            return ParseAttempt.formatError(e.getMessage());
        }
    }

    private void validateRootFields(String jsonObject) throws IOException {
        try {
            JSONObject data = new JSONObject(jsonObject);
            String[] requiredFields = {"factions", "cards", "nodes", "totalNodes", "fallbackUsed"};
            for (String field : requiredFields) {
                if (!data.has(field)) {
                    throw new IOException("missing root field: " + field);
                }
            }
        } catch (JSONException e) {
            throw new IOException("invalid JSON object: " + e.getMessage(), e);
        }
    }

    private ParseResult successResult(ParseResult parseResult) {
        Log.d(TAG, "Parse attempt valid, normalizing data");
        parseValidator.normalize(parseResult);
        parseResult.setParseStatus(ParseResult.STATUS_SUCCESS);
        parseResult.setParseMessage("解析成功");
        parseResult.setRetryAllowed(true);
        parseResult.setRequiresTextEdit(false);
        return parseResult;
    }

    private ParseResult validationFailure(String invalidReason) {
        if (invalidReason != null && invalidReason.contains("has no assigned cards")) {
            return missingActionResult("文件存在多阵营，但其中一方缺少行动，建议补充内容后重试");
        }
        if (invalidReason != null && invalidReason.contains("less than 2 factions")) {
            return failureResult(ParseResult.STATUS_FACTION_COUNT_INVALID,
                    "文本只提到一个阵营或缺少明确阵营对立关系",
                    false,
                    true);
        }
        return parseErrorResult("解析结果未通过业务校验：" + invalidReason);
    }

    private ParseResult parseErrorResult(String message) {
        return failureResult(ParseResult.STATUS_PARSE_ERROR, message, true, false);
    }

    private ParseResult missingActionResult(String message) {
        return failureResult(ParseResult.STATUS_MISSING_ACTION, message, false, true);
    }

    private boolean shouldRetryValidationError(String invalidReason) {
        if (invalidReason == null) {
            return false;
        }
        if (invalidReason.contains("has no assigned cards")
                || invalidReason.contains("less than 2 factions")) {
            return false;
        }
        return invalidReason.contains("duplicate")
                || invalidReason.contains("missing")
                || invalidReason.contains("nodeIndex")
                || invalidReason.contains("totalNodes")
                || invalidReason.contains("invalid node");
    }

    private boolean isEmptyModelOutput(ParseResult parseResult) {
        return parseResult != null
                && parseResult.isFallbackUsed()
                && parseResult.getFactions().isEmpty()
                && parseResult.getCards().isEmpty()
                && parseResult.getNodes().isEmpty();
    }

    private ParseResult failureResult(String status, String message,
                                      boolean retryAllowed, boolean requiresTextEdit) {
        ParseResult result = new ParseResult();
        result.setParseStatus(status);
        result.setParseMessage(message);
        result.setFallbackUsed(false);
        result.setRetryAllowed(retryAllowed);
        result.setRequiresTextEdit(requiresTextEdit);
        result.setTotalNodes(0);
        return result;
    }

    private ParseResult fallbackResult(String message) {
        ParseResult result = fallbackDataProvider.getFallbackData();
        result.setParseStatus(ParseResult.STATUS_PARSE_ERROR);
        result.setParseMessage(message);
        result.setRetryAllowed(true);
        result.setRequiresTextEdit(false);
        return result;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
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

    /**
     * 清洗 JSON 中字符串外的中文标点（避免破坏字符串值内的正常中文内容）。
     * 只替换引号外的：中文逗号，→ , ；中文冒号 ：→ : 。
     */
    private String cleanJsonPunctuation(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        StringBuilder sb = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                sb.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                sb.append(c);
                continue;
            }
            if (!inString) {
                if (c == '，') {
                    sb.append(',');
                    continue;
                }
                if (c == '：') {
                    sb.append(':');
                    continue;
                }
            }
            sb.append(c);
        }
        String cleaned = sb.toString();
        if (!cleaned.equals(json)) {
            Log.d(TAG, "JSON punctuation cleaned (full-width -> half-width outside strings)");
        }
        return cleaned;
    }

    private static class ParseAttempt {
        private final ParseResult parseResult;
        private final String errorMessage;

        private ParseAttempt(ParseResult parseResult, String errorMessage) {
            this.parseResult = parseResult;
            this.errorMessage = errorMessage;
        }

        static ParseAttempt success(ParseResult parseResult) {
            return new ParseAttempt(parseResult, null);
        }

        static ParseAttempt formatError(String errorMessage) {
            return new ParseAttempt(null, errorMessage);
        }

        boolean hasFormatError() {
            return errorMessage != null;
        }
    }
}
