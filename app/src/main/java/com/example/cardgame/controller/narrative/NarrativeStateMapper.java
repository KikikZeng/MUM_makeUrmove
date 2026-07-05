package com.example.cardgame.controller.narrative;

import com.example.cardgame.dto.narrative.NarrativeGameViewData;
import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.dto.narrative.PreviewViewData;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.GameStatus;
import com.example.cardgame.model.narrative.NarrativeGameState;
import com.example.cardgame.model.narrative.NarrativeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NarrativeStateMapper {

    public PreviewViewData toPreviewViewData(String gameId, String rawText, ParseResult parseResult) {
        if (parseResult == null) {
            return null;
        }
        PreviewViewData viewData = new PreviewViewData();
        viewData.setGameId(gameId);
        viewData.setFactions(parseResult.getFactions());
        viewData.setCards(parseResult.getCards());
        viewData.setNodes(parseResult.getNodes());
        viewData.setCardCountByFaction(countCardsByFaction(parseResult.getCards()));
        viewData.setRawText(rawText);
        viewData.setTotalNodes(parseResult.getTotalNodes());
        viewData.setFallbackUsed(parseResult.isFallbackUsed());
        viewData.setParseStatus(parseResult.getParseStatus());
        viewData.setParseMessage(parseResult.getParseMessage());
        viewData.setRetryAllowed(parseResult.isRetryAllowed());
        viewData.setRequiresTextEdit(parseResult.isRequiresTextEdit());
        return viewData;
    }

    public NarrativeGameViewData toGameViewData(NarrativeGameState gameState) {
        if (gameState == null) {
            return null;
        }
        NarrativeGameViewData viewData = new NarrativeGameViewData();
        viewData.setUserFactionId(gameState.getUserFactionId());
        viewData.setFactions(gameState.getFactions());
        viewData.setHandCards(resolveHandCards(gameState));
        viewData.setStageTitles(resolveStageTitles(gameState.getNodes()));
        viewData.setGlobalProgress(gameState.getGlobalProgress());
        viewData.setTotalNodes(gameState.getNodes().size());
        viewData.setHearts(gameState.getHearts());
        viewData.setCorrectCount(gameState.getCorrectCount());
        viewData.setWrongCount(gameState.getWrongCount());
        viewData.setIncompleteCount(gameState.getIncompleteCount());
        viewData.setSevereErrorCount(gameState.getSevereErrorCount());
        viewData.setAiProgress(gameState.getAiProgress());
        viewData.setLastResolvedNodeIndex(gameState.getLastResolvedNodeIndex());
        viewData.setStatus(gameState.getStatus());
        viewData.setGameOver(gameState.getStatus() == GameStatus.FINISHED
                || gameState.getStatus() == GameStatus.ABANDONED);
        applyCurrentNode(viewData, gameState);
        applyResolvedNode(viewData, gameState);
        return viewData;
    }

    private List<String> resolveStageTitles(List<NarrativeNode> nodes) {
        List<String> titles = new ArrayList<>();
        if (nodes == null) {
            return titles;
        }
        for (NarrativeNode node : nodes) {
            titles.add(node.getStageTitle());
        }
        return titles;
    }

    private Map<String, Integer> countCardsByFaction(List<EventCard> cards) {
        Map<String, Integer> counts = new HashMap<>();
        if (cards == null) {
            return counts;
        }
        for (EventCard card : cards) {
            String factionId = card.getFactionId();
            Integer currentCount = counts.get(factionId);
            counts.put(factionId, currentCount == null ? 1 : currentCount + 1);
        }
        return counts;
    }

    private List<EventCard> resolveHandCards(NarrativeGameState gameState) {
        Map<String, EventCard> cardsById = new HashMap<>();
        for (EventCard card : gameState.getCards()) {
            cardsById.put(card.getId(), card);
        }

        List<EventCard> handCards = new ArrayList<>();
        for (String cardId : gameState.getUserHand()) {
            EventCard card = cardsById.get(cardId);
            if (card != null) {
                handCards.add(card);
            }
        }
        return handCards;
    }

    private Map<String, EventCard> buildCardsById(NarrativeGameState gameState) {
        Map<String, EventCard> cardsById = new HashMap<>();
        for (EventCard card : gameState.getCards()) {
            cardsById.put(card.getId(), card);
        }
        return cardsById;
    }

    private Map<String, List<EventCard>> resolveNodeEvents(NarrativeGameState gameState, NarrativeNode node) {
        Map<String, EventCard> cardsById = buildCardsById(gameState);
        Map<String, List<EventCard>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : node.getFactionCardIds().entrySet()) {
            List<EventCard> cards = new ArrayList<>();
            for (String cardId : entry.getValue()) {
                EventCard card = cardsById.get(cardId);
                if (card != null) {
                    cards.add(card);
                }
            }
            result.put(entry.getKey(), cards);
        }
        return result;
    }

    private void applyCurrentNode(NarrativeGameViewData viewData, NarrativeGameState gameState) {
        int progress = gameState.getGlobalProgress();
        if (progress < 0 || progress >= gameState.getNodes().size()) {
            return;
        }
        NarrativeNode currentNode = gameState.getNodes().get(progress);
        viewData.setCurrentNodeEvents(resolveNodeEvents(gameState, currentNode));
        viewData.setStageTitle(currentNode.getStageTitle());
        viewData.setStageHint(currentNode.getStageHint());
        viewData.setOpeningNarration(currentNode.getOpeningNarration());
        viewData.setResultNarration(currentNode.getResultNarration());
        viewData.setCurrentUserNodeEmpty(
                currentNode.getCardIdsForFaction(gameState.getUserFactionId()).isEmpty()
        );
    }

    private void applyResolvedNode(NarrativeGameViewData viewData, NarrativeGameState gameState) {
        int resolvedIndex = gameState.getLastResolvedNodeIndex();
        if (resolvedIndex < 0 || resolvedIndex >= gameState.getNodes().size()) {
            return;
        }
        viewData.setLastResolvedNodeEvents(resolveNodeEvents(gameState, gameState.getNodes().get(resolvedIndex)));
    }
}
