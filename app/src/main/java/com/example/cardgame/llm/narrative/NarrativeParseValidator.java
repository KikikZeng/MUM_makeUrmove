package com.example.cardgame.llm.narrative;

import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.NarrativeNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NarrativeParseValidator {
    private final FallbackNarrativeDataProvider fallbackDataProvider;

    public NarrativeParseValidator() {
        this(new FallbackNarrativeDataProvider());
    }

    public NarrativeParseValidator(FallbackNarrativeDataProvider fallbackDataProvider) {
        this.fallbackDataProvider = fallbackDataProvider;
    }

    public ParseResult validateOrFallback(ParseResult parseResult) {
        if (!isValid(parseResult)) {
            return fallbackDataProvider.getFallbackData();
        }
        normalize(parseResult);
        return parseResult;
    }

    private boolean isValid(ParseResult parseResult) {
        if (parseResult == null
                || parseResult.getFactions() == null || parseResult.getFactions().isEmpty()
                || parseResult.getCards() == null || parseResult.getCards().isEmpty()
                || parseResult.getNodes() == null || parseResult.getNodes().isEmpty()) {
            return false;
        }

        Set<String> factionIds = collectFactionIds(parseResult.getFactions());
        if (factionIds.size() != parseResult.getFactions().size()) {
            return false;
        }

        Map<String, EventCard> cardsById = collectCardsById(parseResult.getCards(), factionIds);
        if (cardsById.size() != parseResult.getCards().size()) {
            return false;
        }

        for (NarrativeNode node : parseResult.getNodes()) {
            if (!isValidNode(node, factionIds, cardsById)) {
                return false;
            }
        }
        return true;
    }

    private void normalize(ParseResult parseResult) {
        Set<String> factionIds = collectFactionIds(parseResult.getFactions());
        for (NarrativeNode node : parseResult.getNodes()) {
            Map<String, List<String>> normalizedMap = new HashMap<>(node.getFactionCardIds());
            for (String factionId : factionIds) {
                if (!normalizedMap.containsKey(factionId)) {
                    normalizedMap.put(factionId, null);
                }
            }
            node.setFactionCardIds(normalizedMap);
        }
        parseResult.setTotalNodes(parseResult.getNodes().size());
        parseResult.setFallbackUsed(false);
    }

    private Set<String> collectFactionIds(List<Faction> factions) {
        Set<String> factionIds = new HashSet<>();
        for (Faction faction : factions) {
            if (faction == null || isBlank(faction.getId())) {
                return new HashSet<>();
            }
            factionIds.add(faction.getId());
        }
        return factionIds;
    }

    private Map<String, EventCard> collectCardsById(List<EventCard> cards, Set<String> factionIds) {
        Map<String, EventCard> cardsById = new HashMap<>();
        for (EventCard card : cards) {
            if (card == null || isBlank(card.getId()) || isBlank(card.getFactionId())
                    || !factionIds.contains(card.getFactionId())) {
                return new HashMap<>();
            }
            cardsById.put(card.getId(), card);
        }
        return cardsById;
    }

    private boolean isValidNode(NarrativeNode node, Set<String> factionIds, Map<String, EventCard> cardsById) {
        if (node == null || node.getFactionCardIds() == null) {
            return false;
        }

        boolean hasAnyCard = false;
        for (Map.Entry<String, List<String>> entry : node.getFactionCardIds().entrySet()) {
            String factionId = entry.getKey();
            if (!factionIds.contains(factionId)) {
                return false;
            }
            List<String> cardIds = entry.getValue();
            if (cardIds == null) {
                continue;
            }
            for (String cardId : cardIds) {
                EventCard card = cardsById.get(cardId);
                if (card == null || !factionId.equals(card.getFactionId())) {
                    return false;
                }
                hasAnyCard = true;
            }
        }
        return hasAnyCard;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
