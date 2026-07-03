package com.example.cardgame.controller.narrative;

import com.example.cardgame.dto.narrative.NarrativeGameViewData;
import com.example.cardgame.dto.narrative.NarrativePlayResult;
import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.dto.narrative.PreviewViewData;
import com.example.cardgame.engine.narrative.NarrativeGameEngine;
import com.example.cardgame.llm.narrative.NarrativeTextParser;
import com.example.cardgame.model.narrative.Faction;

import java.util.List;
import java.util.UUID;

public class NarrativeGameController implements NarrativeActionHandler {
    private final NarrativeTextParser textParser;
    private final NarrativeGameEngine gameEngine;
    private final NarrativeStateMapper stateMapper;

    private String gameId;
    private String currentRawText;
    private ParseResult currentParseResult;

    public NarrativeGameController() {
        this(new NarrativeTextParser(), new NarrativeGameEngine(), new NarrativeStateMapper());
    }

    public NarrativeGameController(NarrativeTextParser textParser,
                                   NarrativeGameEngine gameEngine,
                                   NarrativeStateMapper stateMapper) {
        this.textParser = textParser;
        this.gameEngine = gameEngine;
        this.stateMapper = stateMapper;
    }

    @Override
    public ParseResult parseText(String rawText) {
        this.gameId = UUID.randomUUID().toString();
        this.currentRawText = rawText;
        this.currentParseResult = textParser.parse(rawText);
        return currentParseResult;
    }

    @Override
    public PreviewViewData getPreviewViewData() {
        return stateMapper.toPreviewViewData(gameId, currentRawText, currentParseResult);
    }

    @Override
    public NarrativeGameViewData startNarrativeGame(String selectedFactionId) {
        if (currentParseResult == null) {
            parseText("安史之乱");
        }

        if (!isValidFactionId(selectedFactionId)) {
            NarrativeGameViewData viewData = new NarrativeGameViewData();
            viewData.setMessage("INVALID_FACTION");
            return viewData;
        }

        gameEngine.initializeGame(
                currentParseResult.getFactions(),
                currentParseResult.getCards(),
                currentParseResult.getNodes(),
                selectedFactionId
        );
        return stateMapper.toGameViewData(gameEngine.getGameState());
    }

    @Override
    public NarrativePlayResult submitEventCards(List<String> selectedCardIds) {
        return gameEngine.playCards(selectedCardIds);
    }

    @Override
    public NarrativeGameViewData getNarrativeGameViewData() {
        return stateMapper.toGameViewData(gameEngine.getGameState());
    }

    @Override
    public NarrativeGameViewData abandonGame() {
        gameEngine.abandonGame();
        return stateMapper.toGameViewData(gameEngine.getGameState());
    }

    private boolean isValidFactionId(String selectedFactionId) {
        if (selectedFactionId == null || currentParseResult == null) {
            return false;
        }
        for (Faction faction : currentParseResult.getFactions()) {
            if (selectedFactionId.equals(faction.getId())) {
                return true;
            }
        }
        return false;
    }
}
