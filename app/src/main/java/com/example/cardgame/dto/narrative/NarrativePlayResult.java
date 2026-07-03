package com.example.cardgame.dto.narrative;

import com.example.cardgame.model.narrative.NarrativeGameState;
import com.example.cardgame.rule.narrative.NarrativePlayType;

public class NarrativePlayResult {
    private final boolean success;
    private final NarrativePlayType playType;
    private final String message;
    private final NarrativeGameState gameState;

    public NarrativePlayResult(boolean success, NarrativePlayType playType,
                               String message, NarrativeGameState gameState) {
        this.success = success;
        this.playType = playType;
        this.message = message;
        this.gameState = gameState;
    }

    public boolean isSuccess() {
        return success;
    }

    public NarrativePlayType getPlayType() {
        return playType;
    }

    public String getMessage() {
        return message;
    }

    public NarrativeGameState getGameState() {
        return gameState;
    }
}
