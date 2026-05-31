package com.example.cardgame.llm.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DeepSeekRequest {
    private String model = "deepseek-chat";
    private List<ChatMessage> messages;
    @SerializedName("max_tokens")
    private int maxTokens = 500;
    private double temperature = 0.7;

    // Getters and setters
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}