package com.example.cardgame.llm;

import android.util.Log;

import com.example.cardgame.llm.model.ChatMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LLMAnalyzer {
    private static final String TAG = "LLMAnalyzer";
    private final VivoLLMClient vivoClient;

    public LLMAnalyzer() {
        Log.d(TAG, "LLMAnalyzer 初始化");
        this.vivoClient = new VivoLLMClient();
    }

    public String analyzeGameSituation(String gameStateDescription) throws IOException {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "You are a helpful assistant for a card game analysis."));
        messages.add(new ChatMessage("user", "Analyze this game situation: " + gameStateDescription));

        return vivoClient.chat(messages);
    }

    public String suggestPlay(String handCards, String lastPlay, String gameProgress) throws IOException {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "You are an expert card game strategy advisor."));
        messages.add(new ChatMessage("user", buildStrategyPrompt(handCards, lastPlay, gameProgress)));

        return vivoClient.chat(messages);
    }

    public String analyzeHand(String handCards) {
        String prompt = "你是一个锄大地专家。请分析以下手牌并给出简短的战略建议：\n" + handCards;
        try {
            List<ChatMessage> messages = Collections.singletonList(new ChatMessage("user", prompt));
            return vivoClient.chat(messages);
        } catch (IOException e) {
            e.printStackTrace();
            return "分析失败：" + e.getMessage();
        }
    }

    private String buildStrategyPrompt(String handCards, String lastPlay, String gameProgress) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("My hand cards: ").append(handCards).append("\n");
        prompt.append("Last play: ").append(lastPlay).append("\n");
        prompt.append("Game progress: ").append(gameProgress).append("\n");
        prompt.append("What card should I play? Provide your recommendation.");
        return prompt.toString();
    }

    public String summarizeOpponentStyle(String playHistory) throws IOException {
        Log.d(TAG, "开始分析对手风格，历史记录: " + playHistory);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "You are an expert at analyzing card game player behavior. Analyze the opponent's play style based on their history and respond with ONLY ONE of these labels: 激进 (aggressive), 保守 (defensive), or 均衡 (balanced). Consider: Does the player frequently use big cards to beat others? Do they prefer to pass and wait? Do they play both big and small cards?"));
        messages.add(new ChatMessage("user", "Opponent play history: " + playHistory + "\n\nWhat is this opponent's play style? Respond with just one word: 激进, 保守, or 均衡."));

        Log.d(TAG, "发起 LLM 请求");
        String result = vivoClient.chat(messages);
        Log.d(TAG, "LLM 返回结果: " + result);
        return result;
    }
}