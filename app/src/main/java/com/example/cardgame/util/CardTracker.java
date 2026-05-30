package com.example.cardgame.util;

import com.example.cardgame.model.Card;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 记牌器 - 记录每张牌是否已打出、由谁打出
 * 用于对手手牌采样时排除已知不可能出现的牌
 */
public class CardTracker {
    private Set<Card> playedCards = new HashSet<>();
    private Map<Card, String> playedBy = new HashMap<>();

    public void onCardPlayed(Card card, String playerId) {
        playedCards.add(card);
        playedBy.put(card, playerId);
    }

    public void onCardsPlayed(List<Card> cards, String playerId) {
        for (Card card : cards) {
            onCardPlayed(card, playerId);
        }
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
    }

    public int getPlayedCount() {
        return playedCards.size();
    }
}