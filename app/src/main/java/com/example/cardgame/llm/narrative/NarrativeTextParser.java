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
                result.setParseStatus(ParseResult.STATUS_PARSE_ERROR);
            } else {
                Log.i(TAG, "=== 解析成功 ===");
                Log.i(TAG, "Factions count: " + (result.getFactions() != null ? result.getFactions().size() : 0));
                Log.i(TAG, "Cards count: " + (result.getCards() != null ? result.getCards().size() : 0));
                Log.i(TAG, "Nodes count: " + (result.getNodes() != null ? result.getNodes().size() : 0));
                result.setParseStatus(ParseResult.STATUS_SUCCESS);
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "=== 解析失败，使用 Fallback 数据 ===");
            Log.e(TAG, "Exception: " + e.getMessage(), e);
            Log.e(TAG, "Fallback data: 安史之乱预设数据");
            ParseResult fallback = fallbackDataProvider.getFallbackData();
            fallback.setParseStatus(ParseResult.STATUS_PARSE_ERROR);
            return fallback;
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
            Log.e(TAG, "First attempt failed: " + e.getMessage(), e);
            ParseResult fallback = fallbackDataProvider.getFallbackData();
            fallback.setParseStatus(ParseResult.STATUS_PARSE_ERROR);
            return fallback;
        }

        String invalidReason = parseValidator.getInvalidReason(parseResult);
        if (invalidReason != null) {
            Log.w(TAG, "First parse attempt invalid: " + invalidReason);

            if (isFactionCountIssue(invalidReason)) {
                Log.e(TAG, "阵营数量不符合要求，直接返回错误");
                parseResult.setParseStatus(ParseResult.STATUS_FACTION_COUNT_INVALID);
                parseResult.setErrorMessage(invalidReason);
                parseResult.setFallbackUsed(false);
                return parseResult;
            }

            Log.w(TAG, "Retrying with temperature " + RETRY_TEMPERATURE + "...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            Log.d(TAG, "Second attempt with temperature " + RETRY_TEMPERATURE);
            content = llmClient.chat(promptBuilder.buildMessages(sanitizedText), RETRY_TEMPERATURE);
            Log.d(TAG, "Raw LLM response (retry) length: " + (content != null ? content.length() : 0));

            try {
                String jsonObject = extractJsonObject(content);
                jsonObject = cleanJsonPunctuation(jsonObject);
                parseResult = gson.fromJson(jsonObject, ParseResult.class);
            } catch (Exception e) {
                Log.e(TAG, "Second attempt failed: " + e.getMessage(), e);
                ParseResult fallback = fallbackDataProvider.getFallbackData();
                fallback.setParseStatus(ParseResult.STATUS_PARSE_ERROR);
                return fallback;
            }

            String retryInvalidReason = parseValidator.getInvalidReason(parseResult);
            if (retryInvalidReason != null) {
                Log.e(TAG, "Second parse attempt invalid: " + retryInvalidReason);
                parseResult.setParseStatus(ParseResult.STATUS_MISSING_ACTION);
                parseResult.setErrorMessage(retryInvalidReason);
                parseResult.setFallbackUsed(false);
                return parseResult;
            }
            Log.d(TAG, "Second parse attempt valid, normalizing data");
            parseValidator.normalize(parseResult);
            parseResult.setParseStatus(ParseResult.STATUS_SUCCESS);
            return parseResult;
        }

        Log.d(TAG, "First parse attempt valid, normalizing data");
        parseValidator.normalize(parseResult);
        parseResult.setParseStatus(ParseResult.STATUS_SUCCESS);
        return parseResult;
    }

    private boolean isFactionCountIssue(String invalidReason) {
        return invalidReason != null && (
                invalidReason.contains("less than 2 factions")
                        || invalidReason.contains("missing required fields")
                        || invalidReason.contains("empty factions")
        );
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
}
