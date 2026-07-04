package com.example.cardgame.llm;

import android.util.Log;

import com.example.cardgame.llm.model.ChatMessage;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("SpellCheckingInspection")
public class VivoLLMClient {

    private static final String TAG = "VivoLLMClient";

    // 官方 API 地址
    private static final String BASE_URL = "https://api-ai.vivo.com.cn/v1/chat/completions";

    // 使用轻量级 Mini 模型，速度快且成本低
    private static final String MODEL_NAME = "Doubao-Seed-2.0-mini";

    // 硬编码 API Key（参赛专用，私有仓库安全）
    private static final String API_KEY = "sk-xuanji-2026006675-cnl6U3V3QlVsTVBCZUlBeA==";

    private final OkHttpClient client;
    private final Gson gson;   // 仍保留，但只用于深度解析（未用）

    public VivoLLMClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    // 默认温度 0.2，适合结构化输出
    public String chat(List<ChatMessage> messages) throws IOException {
        return chat(messages, 0.2);
    }

    // 主方法，支持自定义温度
    public String chat(List<ChatMessage> messages, double temperature) throws IOException {
        Log.d(TAG, "=== 开始调用 vivo 蓝心大模型 ===");
        Log.d(TAG, "Model: " + MODEL_NAME + ", temperature: " + temperature);
        Log.d(TAG, "Messages count: " + messages.size());

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            String contentPreview = msg.getContent().length() > 200
                    ? msg.getContent().substring(0, 200) + "..."
                    : msg.getContent();
            Log.d(TAG, "Message[" + i + "] role=" + msg.getRole() + ", content=" + contentPreview);
        }

        String requestId = UUID.randomUUID().toString();

        // 构建 JSON 请求体
        JSONObject body = new JSONObject();
        try {
            body.put("model", MODEL_NAME);
            body.put("temperature", temperature);
            body.put("max_tokens", 4096);
            body.put("stream", false);
            // 可选：开启轻量推理，改善逻辑
            // body.put("reasoning_effort", "low");

            JSONArray messagesArray = new JSONArray();
            for (ChatMessage msg : messages) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.getRole());
                msgObj.put("content", msg.getContent());
                messagesArray.put(msgObj);
            }
            body.put("messages", messagesArray);

        } catch (JSONException e) {
            throw new IOException("Failed to build JSON request: " + e.getMessage(), e);
        }

        okhttp3.HttpUrl url = okhttp3.HttpUrl.parse(BASE_URL)
                .newBuilder()
                .addQueryParameter("request_id", requestId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        Log.d(TAG, "Sending request to vivo LLM, requestId: " + requestId);

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                Log.e(TAG, "API request failed, code: " + response.code() + ", body: " + responseBody);
                throw new IOException("API request failed with code: " + response.code() + ", body: " + responseBody);
            }

            // 解析响应，提取 content
            try {
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() == 0) {
                    throw new IOException("No choices in response");
                }
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.getJSONObject("message");
                String content = message.getString("content");

                Log.d(TAG, "=== vivo 蓝心大模型调用成功 ===");
                Log.d(TAG, "Response content length: " + (content != null ? content.length() : 0));
                String preview = content != null && content.length() > 500 ? content.substring(0, 500) + "..." : content;
                Log.d(TAG, "Response preview: " + preview);

                return content;

            } catch (JSONException e) {
                throw new IOException("Failed to parse response JSON: " + e.getMessage() + ", body: " + responseBody, e);
            }
        }
    }
}