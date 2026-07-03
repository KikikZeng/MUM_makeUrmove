package com.example.cardgame.llm;

import android.util.Log;

import com.example.cardgame.BuildConfig;
import com.example.cardgame.llm.model.ChatMessage;
import com.example.cardgame.llm.model.DeepSeekResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GlmLLMClient {
    private static final String TAG = "GlmLLMClient";
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    private static final String DEFAULT_MODEL = "glm-4-flash";

    private final OkHttpClient client;
    private final Gson gson;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public GlmLLMClient() {
        this(BuildConfig.DEEPSEEK_API_KEY, DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    public GlmLLMClient(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.model = model;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        Log.d(TAG, "GLM_API_KEY length: " + (apiKey == null ? 0 : apiKey.length()));
    }

    public String chat(List<ChatMessage> messages) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("GLM_API_KEY is empty");
        }

        GlmChatRequest requestBody = new GlmChatRequest();
        requestBody.setModel(model);
        requestBody.setMessages(messages);
        requestBody.setTemperature(0.2);
        requestBody.setTopP(0.9);
        requestBody.setMaxTokens(4096);

        String json = gson.toJson(requestBody);
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "null";
                throw new IOException("Unexpected code " + response + ", body: " + errorBody);
            }
            String responseBody = response.body() != null ? response.body().string() : null;
            if (responseBody == null) {
                throw new IOException("Empty response body from GLM API");
            }
            DeepSeekResponse glmResponse = gson.fromJson(responseBody, DeepSeekResponse.class);
            if (glmResponse == null || glmResponse.getContent() == null) {
                throw new IOException("Empty content from GLM API");
            }
            return glmResponse.getContent();
        }
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "https://open.bigmodel.cn/api/paas/v4";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static class GlmChatRequest {
        private String model;
        private List<ChatMessage> messages;
        private double temperature;
        private double top_p;
        private int max_tokens;

        public void setModel(String model) {
            this.model = model;
        }

        public void setMessages(List<ChatMessage> messages) {
            this.messages = messages;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public void setTopP(double topP) {
            this.top_p = topP;
        }

        public void setMaxTokens(int maxTokens) {
            this.max_tokens = maxTokens;
        }
    }
}
