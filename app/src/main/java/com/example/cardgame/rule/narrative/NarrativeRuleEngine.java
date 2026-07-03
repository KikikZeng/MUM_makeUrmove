package com.example.cardgame.rule.narrative;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NarrativeRuleEngine {

    public NarrativeValidationResult validatePlay(List<String> selectedCardIds, List<String> currentNodeCardIds) {
        Set<String> selected = toSet(selectedCardIds);
        Set<String> required = toSet(currentNodeCardIds);

        if (required.isEmpty()) {
            if (selected.isEmpty()) {
                return new NarrativeValidationResult(
                        NarrativePlayType.PERFECT,
                        new ArrayList<String>(),
                        new ArrayList<String>(),
                        "EMPTY_NODE_OK"
                );
            }
            return wrong("EMPTY_NODE_REJECTS_CARDS");
        }

        if (selected.isEmpty()) {
            return wrong("NO_CARD_SELECTED");
        }

        if (!required.containsAll(selected)) {
            return wrong("SELECTED_CARD_NOT_IN_CURRENT_NODE");
        }

        if (selected.equals(required)) {
            return new NarrativeValidationResult(
                    NarrativePlayType.PERFECT,
                    new ArrayList<>(selected),
                    new ArrayList<String>(),
                    "PERFECT"
            );
        }

        Set<String> shattered = new HashSet<>(required);
        shattered.removeAll(selected);
        return new NarrativeValidationResult(
                NarrativePlayType.INCOMPLETE,
                new ArrayList<>(selected),
                new ArrayList<>(shattered),
                "INCOMPLETE"
        );
    }

    private NarrativeValidationResult wrong(String message) {
        return new NarrativeValidationResult(
                NarrativePlayType.WRONG,
                new ArrayList<String>(),
                new ArrayList<String>(),
                message
        );
    }

    private Set<String> toSet(List<String> cardIds) {
        Set<String> result = new HashSet<>();
        if (cardIds == null) {
            return result;
        }
        for (String cardId : cardIds) {
            if (cardId != null && !cardId.trim().isEmpty()) {
                result.add(cardId);
            }
        }
        return result;
    }
}
