package com.example.cardgame.llm.model;

import java.util.List;

public class DeepSeekResponse {
    private List<Choice> choices;

    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getMessage().getContent();
        }
        return null;
    }

    public static class Choice {
        private ChatMessage message;
        public ChatMessage getMessage() { return message; }
        public void setMessage(ChatMessage message) { this.message = message; }
    }

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
}