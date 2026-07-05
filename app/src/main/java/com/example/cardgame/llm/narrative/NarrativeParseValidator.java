package com.example.cardgame.llm.narrative;

import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.NarrativeNode;

import java.util.ArrayList;
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

    public boolean isValid(ParseResult parseResult) {
        return getInvalidReason(parseResult) == null;
    }

    public String getInvalidReason(ParseResult parseResult) {
        if (parseResult == null
                || parseResult.getFactions() == null || parseResult.getFactions().isEmpty()
                || parseResult.getCards() == null || parseResult.getCards().isEmpty()
                || parseResult.getNodes() == null || parseResult.getNodes().isEmpty()) {
            return "missing required fields or empty factions/cards/nodes";
        }
        if (parseResult.getFactions().size() < 2 || parseResult.getFactions().size() > 4) {
            return "faction count must be between 2 and 4";
        }
        if (parseResult.getTotalNodes() != parseResult.getNodes().size()) {
            return "totalNodes does not match nodes size";
        }

        Set<String> factionIds = collectFactionIds(parseResult.getFactions());
        if (factionIds.size() != parseResult.getFactions().size()) {
            return "blank or duplicate faction id";
        }

        Map<String, EventCard> cardsById = collectCardsById(parseResult.getCards(), factionIds);
        if (cardsById.size() != parseResult.getCards().size()) {
            return "blank, duplicate, or unknown factionId in cards";
        }
        for (EventCard card : parseResult.getCards()) {
            if (isBlank(card.getEventTime())) {
                return "card missing eventTime";
            }
            if (!isValidEventTime(card.getEventTime())) {
                return "card invalid eventTime";
            }
        }
        for (int i = 0; i < parseResult.getNodes().size(); i++) {
            NarrativeNode node = parseResult.getNodes().get(i);
            if (node == null || node.getNodeIndex() != i) {
                return "nodeIndex is not continuous from 0";
            }
            if (!isValidNode(node, factionIds, cardsById)) {
                return "invalid node at index " + i;
            }
        }
        Set<String> assignedFactionIds = collectAssignedFactionIds(parseResult.getNodes());
        if (assignedFactionIds.size() < 2) {
            return "less than 2 factions have assigned cards";
        }
        for (String factionId : factionIds) {
            if (!assignedFactionIds.contains(factionId)) {
                return "faction " + factionId + " has no assigned cards";
            }
        }
        return null;
    }

    public void normalize(ParseResult parseResult) {
        removeUnassignedFactionsAndCards(parseResult);
        Set<String> factionIds = collectFactionIds(parseResult.getFactions());
        normalizeCardText(parseResult.getCards());
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

    private void removeUnassignedFactionsAndCards(ParseResult parseResult) {
        Set<String> assignedFactionIds = collectAssignedFactionIds(parseResult.getNodes());
        Set<String> referencedCardIds = collectReferencedCardIds(parseResult.getNodes(), assignedFactionIds);

        List<Faction> keptFactions = new ArrayList<>();
        for (Faction faction : parseResult.getFactions()) {
            if (faction != null && assignedFactionIds.contains(faction.getId())) {
                keptFactions.add(faction);
            }
        }
        parseResult.setFactions(keptFactions);

        List<EventCard> keptCards = new ArrayList<>();
        for (EventCard card : parseResult.getCards()) {
            if (card != null
                    && assignedFactionIds.contains(card.getFactionId())
                    && referencedCardIds.contains(card.getId())) {
                keptCards.add(card);
            }
        }
        parseResult.setCards(keptCards);

        for (NarrativeNode node : parseResult.getNodes()) {
            Map<String, List<String>> keptMap = new HashMap<>();
            for (String factionId : assignedFactionIds) {
                keptMap.put(factionId, node.getCardIdsForFaction(factionId));
            }
            node.setFactionCardIds(keptMap);
        }
    }

    private void normalizeCardText(List<EventCard> cards) {
        if (cards == null) {
            return;
        }
        for (EventCard card : cards) {
            if (card == null) {
                continue;
            }
            String title = trimToEmpty(card.getTitle());
            String summary = trimToEmpty(card.getSummary());
            String sourceHint = trimToEmpty(card.getSourceHint());

            if (isSameText(title, sourceHint)) {
                card.setSourceHint(!isBlank(summary) && !isSameText(title, summary)
                        ? summary
                        : "历史关键节点");
            }
            if (isBlank(summary) || isSameText(title, summary)) {
                String updatedSourceHint = trimToEmpty(card.getSourceHint());
                card.setSummary(!isBlank(updatedSourceHint) && !isSameText(title, updatedSourceHint)
                        ? updatedSourceHint
                        : "推动历史进程");
            }
        }
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

    private int countAssignedFactions(List<NarrativeNode> nodes) {
        return collectAssignedFactionIds(nodes).size();
    }

    private Set<String> collectAssignedFactionIds(List<NarrativeNode> nodes) {
        Set<String> factionsWithCards = new HashSet<>();
        for (NarrativeNode node : nodes) {
            if (node == null || node.getFactionCardIds() == null) {
                continue;
            }
            for (Map.Entry<String, List<String>> entry : node.getFactionCardIds().entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    factionsWithCards.add(entry.getKey());
                }
            }
        }
        return factionsWithCards;
    }

    private Set<String> collectReferencedCardIds(List<NarrativeNode> nodes, Set<String> factionIds) {
        Set<String> referencedCardIds = new HashSet<>();
        for (NarrativeNode node : nodes) {
            if (node == null || node.getFactionCardIds() == null) {
                continue;
            }
            for (String factionId : factionIds) {
                referencedCardIds.addAll(node.getCardIdsForFaction(factionId));
            }
        }
        return referencedCardIds;
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

    private boolean isValidEventTime(String value) {
        String eventTime = trimToEmpty(value);
        if (isBlank(eventTime)) {
            return false;
        }
        if ("\u65f6\u95f4\u4e0d\u8be6".equals(eventTime)) {
            return true;
        }
        if (eventTime.length() > 12) {
            return false;
        }
        if (eventTime.matches(".*[0-9\u96f6\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341]+.*[\u5e74\u6708\u65e5\u4e16\u7eaa\u4ee3].*")) {
            return true;
        }
        if (eventTime.matches(".*(\u65f6\u671f|\u65f6\u4ee3|\u671d|\u4ee3|\u6625\u79cb|\u6218\u56fd|\u6c11\u56fd).*")) {
            return true;
        }
        return eventTime.matches(".*(\u6e05\u672b|\u5510\u521d|\u660e\u672b|\u6c49\u672b|\u5b8b\u672b|\u6218\u524d|\u6218\u540e|\u4e4b\u524d|\u4e4b\u540e|\u968f\u540e|\u6b64\u540e|\u540c\u5e74|\u6b21\u5e74|\u5f53\u5e74|\u5e74\u521d|\u5e74\u672b|\u521d\u671f|\u4e2d\u671f|\u540e\u671f|\u672b\u671f).*");
    }

    private boolean isSameText(String left, String right) {
        return trimToEmpty(left).equals(trimToEmpty(right));
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
