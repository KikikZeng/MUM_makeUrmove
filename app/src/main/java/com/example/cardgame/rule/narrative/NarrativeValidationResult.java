package com.example.cardgame.rule.narrative;

import java.util.ArrayList;
import java.util.List;

public class NarrativeValidationResult {
    private NarrativePlayType playType;
    private List<String> acceptedCardIds;
    private List<String> shatteredCardIds;
    private String message;

    public NarrativeValidationResult(NarrativePlayType playType, List<String> acceptedCardIds,
                                     List<String> shatteredCardIds, String message) {
        this.playType = playType;
        this.acceptedCardIds = acceptedCardIds != null ? new ArrayList<>(acceptedCardIds) : new ArrayList<String>();
        this.shatteredCardIds = shatteredCardIds != null ? new ArrayList<>(shatteredCardIds) : new ArrayList<String>();
        this.message = message;
    }

    public NarrativePlayType getPlayType() {
        return playType;
    }

    public List<String> getAcceptedCardIds() {
        return acceptedCardIds;
    }

    public List<String> getShatteredCardIds() {
        return shatteredCardIds;
    }

    public String getMessage() {
        return message;
    }

    public boolean isPerfect() {
        return playType == NarrativePlayType.PERFECT;
    }

    public boolean isIncomplete() {
        return playType == NarrativePlayType.INCOMPLETE;
    }

    public boolean isWrong() {
        return playType == NarrativePlayType.WRONG;
    }
}
