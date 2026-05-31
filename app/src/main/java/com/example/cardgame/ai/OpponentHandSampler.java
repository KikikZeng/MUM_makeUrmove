package com.example.cardgame.ai;

import com.example.cardgame.model.*;
import com.example.cardgame.util.CardTracker;
import java.util.*;

public class OpponentHandSampler {

    public static class World {
        public final List<List<Card>> opponentHands; // 索引0,1,2
        public World(List<Card> opp1, List<Card> opp2, List<Card> opp3) {
            this.opponentHands = Arrays.asList(opp1, opp2, opp3);
        }
    }

    public List<World> sampleWorlds(Player aiPlayer, GameState gameState, CardTracker cardTracker, int numSamples) {
        List<World> worlds = new ArrayList<>();

        // 所有牌
        Set<Card> allCards = getAllCards();
        // 去掉AI手牌
        allCards.removeAll(aiPlayer.getHandCards());
        // 去掉已打出的牌（使用CardTracker）
        Set<Card> playedCards = new HashSet<>(cardTracker.getPlayedCards());
        allCards.removeAll(playedCards);
        List<Card> remaining = new ArrayList<>(allCards);

        // 各对手手牌张数
        int[] opponentCounts = new int[3];
        int idx = 0;
        for (Player p : gameState.getPlayers()) {
            if (!p.getPlayerId().equals(aiPlayer.getPlayerId())) {
                opponentCounts[idx++] = p.getHandCards().size();
            }
        }

        Random rng = new Random();
        for (int s = 0; s < numSamples; s++) {
            Collections.shuffle(remaining, rng);
            int pos = 0;
            List<Card> opp1 = new ArrayList<>(remaining.subList(pos, pos + opponentCounts[0]));
            pos += opponentCounts[0];
            List<Card> opp2 = new ArrayList<>(remaining.subList(pos, pos + opponentCounts[1]));
            pos += opponentCounts[1];
            List<Card> opp3 = new ArrayList<>(remaining.subList(pos, remaining.size()));
            worlds.add(new World(opp1, opp2, opp3));
        }
        return worlds;
    }

    private Set<Card> getAllCards() {
        Set<Card> set = new HashSet<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                String cardId = suit.name() + "_" + rank.name();
                set.add(new Card(cardId, suit, rank));
            }
        }
        return set;
    }
}