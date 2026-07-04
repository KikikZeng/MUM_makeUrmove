package com.example.cardgame.engine.narrative;

import com.example.cardgame.dto.narrative.NarrativePlayResult;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.GameStatus;
import com.example.cardgame.model.narrative.NarrativeGameState;
import com.example.cardgame.model.narrative.NarrativeNode;
import com.example.cardgame.rule.narrative.NarrativePlayType;
import com.example.cardgame.rule.narrative.NarrativeRuleEngine;
import com.example.cardgame.rule.narrative.NarrativeValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NarrativeGameEngine {
    private final NarrativeRuleEngine ruleEngine;
    private NarrativeGameState gameState;

    public NarrativeGameEngine() {
        this(new NarrativeRuleEngine());
    }

    public NarrativeGameEngine(NarrativeRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public NarrativeGameState initializeGame(List<Faction> factions, List<EventCard> cards,
                                             List<NarrativeNode> nodes, String selectedFactionId) {
        NarrativeGameState state = new NarrativeGameState();
        state.setFactions(factions);
        state.setCards(cards);
        state.setNodes(nodes);
        state.setUserFactionId(selectedFactionId);
        state.setUserHand(buildUserHand(nodes, selectedFactionId));
        state.setUserProgress(0);
        state.setGlobalProgress(0);
        state.setAiProgress(buildInitialAiProgress(factions, selectedFactionId));
        state.setHearts(3.0);
        state.setStatus(GameStatus.PLAYING);
        this.gameState = state;
        return state;
    }

    public NarrativePlayResult playCards(List<String> selectedCardIds) {
        if (gameState == null || gameState.getStatus() != GameStatus.PLAYING) {
            return new NarrativePlayResult(false, NarrativePlayType.WRONG, "GAME_NOT_PLAYING", gameState);
        }
        if (isGameOver()) {
            gameState.setStatus(GameStatus.FINISHED);
            return new NarrativePlayResult(false, NarrativePlayType.WRONG, "GAME_OVER", gameState);
        }

        NarrativeNode currentNode = gameState.getNodes().get(gameState.getGlobalProgress());
        List<String> currentUserNode = currentNode.getCardIdsForFaction(gameState.getUserFactionId());
        NarrativeValidationResult validationResult =
                ruleEngine.validatePlay(selectedCardIds, currentUserNode);

        if (validationResult.isPerfect()) {
            applyPerfect(validationResult.getAcceptedCardIds());
            return new NarrativePlayResult(true, NarrativePlayType.PERFECT, "PERFECT", gameState);
        }
        if (validationResult.isIncomplete()) {
            applyIncomplete(validationResult.getAcceptedCardIds(), validationResult.getShatteredCardIds());
            return new NarrativePlayResult(true, NarrativePlayType.INCOMPLETE, "INCOMPLETE", gameState);
        }

        applyWrong();
        return new NarrativePlayResult(false, NarrativePlayType.WRONG, validationResult.getMessage(), gameState);
    }

    public NarrativeGameState getGameState() {
        return gameState;
    }

    public boolean isGameOver() {
        return gameState != null && gameState.getGlobalProgress() >= gameState.getNodes().size();
    }

    public void abandonGame() {
        if (gameState != null) {
            gameState.setStatus(GameStatus.ABANDONED);
        }
    }

    private void applyPerfect(List<String> acceptedCardIds) {
        gameState.getUserHand().removeAll(acceptedCardIds);
        gameState.getUserSequence().add(new ArrayList<>(acceptedCardIds));
        gameState.setCorrectCount(gameState.getCorrectCount() + 1);
        advanceGlobalNode();
    }

    private void applyIncomplete(List<String> acceptedCardIds, List<String> shatteredCardIds) {
        gameState.getUserHand().removeAll(acceptedCardIds);
        gameState.getUserHand().removeAll(shatteredCardIds);
        gameState.getUserSequence().add(new ArrayList<>(acceptedCardIds));
        gameState.setIncompleteCount(gameState.getIncompleteCount() + 1);
        loseHeart(0.5);
        advanceGlobalNode();
    }

    private void applyWrong() {
        if (gameState.getHearts() <= 0.0) {
            gameState.setSevereErrorCount(gameState.getSevereErrorCount() + 1);
            return;
        }
        gameState.setWrongCount(gameState.getWrongCount() + 1);
        loseHeart(1.0);
    }

    private void loseHeart(double amount) {
        gameState.setHearts(Math.max(0.0, gameState.getHearts() - amount));
        if (gameState.getHearts() <= 0.0) {
            gameState.setStatus(GameStatus.FINISHED);
        }
    }

    private void advanceGlobalNode() {
        gameState.setLastResolvedNodeIndex(gameState.getGlobalProgress());
        int nextProgress = gameState.getGlobalProgress() + 1;
        gameState.setGlobalProgress(nextProgress);
        gameState.setUserProgress(nextProgress);
        Map<String, Integer> aiProgress = new HashMap<>(gameState.getAiProgress());
        for (String factionId : aiProgress.keySet()) {
            aiProgress.put(factionId, nextProgress);
        }
        gameState.setAiProgress(aiProgress);
        if (isGameOver()) {
            gameState.setStatus(GameStatus.FINISHED);
        }
    }

    private List<String> buildUserHand(List<NarrativeNode> nodes, String selectedFactionId) {
        List<String> userHand = new ArrayList<>();
        if (nodes == null) {
            return userHand;
        }
        for (NarrativeNode node : nodes) {
            userHand.addAll(node.getCardIdsForFaction(selectedFactionId));
        }
        Collections.shuffle(userHand);
        return userHand;
    }

    private Map<String, Integer> buildInitialAiProgress(List<Faction> factions, String selectedFactionId) {
        Map<String, Integer> aiProgress = new HashMap<>();
        if (factions == null) {
            return aiProgress;
        }
        for (Faction faction : factions) {
            if (faction != null && faction.getId() != null && !faction.getId().equals(selectedFactionId)) {
                aiProgress.put(faction.getId(), 0);
            }
        }
        return aiProgress;
    }
}