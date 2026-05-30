package com.example.cardgame.util;

import com.example.cardgame.model.Card;
import java.util.*;
import java.util.stream.Collectors;

public class CardTracker {
    private Set<Card> playedCards = new HashSet<>();
    private Map<Card, String> playedBy = new HashMap<>();
    private Map<String, List<String>> opponentHistory = new HashMap<>();

    public void onCardPlayed(Card card, String playerId) {
        playedCards.add(card);
        playedBy.put(card, playerId);
    }

    public void onCardsPlayed(List<Card> cards, String playerId) {
        for (Card card : cards) {
            onCardPlayed(card, playerId);
        }
    }

    public void recordPlay(String playerId, String playDescription) {
        opponentHistory.computeIfAbsent(playerId, k -> new ArrayList<>()).add(playDescription);
    }

    public String getHistorySummary(String playerId) {
        List<String> history = opponentHistory.get(playerId);
        if (history == null || history.isEmpty()) return "";
        int size = history.size();
        int start = size > 20 ? size - 20 : 0;
        List<String> recent = history.subList(start, size);
        return String.join(", ", recent);
    }

    public List<Card> getRemainingCards(List<Card> allCards) {
        return allCards.stream()
                .filter(c -> !playedCards.contains(c))
                .collect(Collectors.toList());
    }

    public boolean isPlayed(Card card) {
        return playedCards.contains(card);
    }

    public Set<Card> getPlayedCards() {
        return new HashSet<>(playedCards);
    }

    public String getPlayedBy(Card card) {
        return playedBy.get(card);
    }

    public void reset() {
        playedCards.clear();
        playedBy.clear();
        opponentHistory.clear();
    }

    public int getPlayedCount() {
        return playedCards.size();
    }
}