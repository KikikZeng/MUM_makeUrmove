package com.example.cardgame.llm;

import android.util.Log;

import com.example.cardgame.BuildConfig;

import com.example.cardgame.llm.model.ChatMessage;

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

public class VivoLLMClient {

    private static final String TAG = "VivoLLMClient";

    private static final String BASE_URL = "https://api-ai.vivo.com.cn/v1/chat/completions";

    private static final String MODEL_NAME = "Doubao-Seed-2.0-mini";

    private final OkHttpClient client;
    private final String apiKey;

    public VivoLLMClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        String key = BuildConfig.VIVO_API_KEY;
        if (key == null || key.isEmpty() || "your_vivo_api_key".equals(key)) {
            key = BuildConfig.DEEPSEEK_API_KEY;
            if (key != null && !key.isEmpty() && !"your_deepseek_api_key".equals(key)) {
                Log.w(TAG, "VIVO_API_KEY not set, using DEEPSEEK_API_KEY as fallback.");
            } else {
                Log.e(TAG, "API Key is not configured! Please set VIVO_API_KEY in local.properties");
            }
        }
        this.apiKey = key;
    }

    public String chat(List<ChatMessage> messages) throws IOException {
        return chat(messages, 0.2);
    }

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

        if (apiKey == null || apiKey.isEmpty() || "your_vivo_api_key".equals(apiKey)) {
            throw new IOException("VIVO_API_KEY is not configured. Please add VIVO_API_KEY=your_key to local.properties");
        }

        String requestId = UUID.randomUUID().toString();

        JSONObject body = new JSONObject();
        try {
            body.put("model", MODEL_NAME);
            body.put("temperature", temperature);
            body.put("max_tokens", 4096);
            body.put("stream", false);

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
                .header("Authorization", "Bearer " + apiKey)
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
                Log.e(TAG, "JSON parsing failed, raw response: " + responseBody);
                throw new IOException("Failed to parse response JSON: " + e.getMessage() + ", body: " + responseBody, e);
            }
        }
    }
}