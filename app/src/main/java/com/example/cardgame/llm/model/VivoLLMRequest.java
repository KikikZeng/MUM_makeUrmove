package com.example.cardgame.llm.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.UUID;

public class VivoLLMRequest {
    private String requestId = UUID.randomUUID().toString();
    private String model;
    private List<ChatMessage> messages;
    @SerializedName("max_tokens")
    private Integer maxTokens = 4096;
    private Double temperature = 0.7;
    private Double topP = 0.7;
    private Boolean stream = false;
    @SerializedName("reasoning_effort")
    private String reasoningEffort;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public String getReasoningEffort() { return reasoningEffort; }
    public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
}
