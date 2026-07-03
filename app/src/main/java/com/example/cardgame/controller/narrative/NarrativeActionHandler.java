package com.example.cardgame.controller.narrative;

import com.example.cardgame.dto.narrative.NarrativeGameViewData;
import com.example.cardgame.dto.narrative.NarrativePlayResult;
import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.dto.narrative.PreviewViewData;

import java.util.List;

public interface NarrativeActionHandler {
    ParseResult parseText(String rawText);

    PreviewViewData getPreviewViewData();

    NarrativeGameViewData startNarrativeGame(String selectedFactionId);

    NarrativePlayResult submitEventCards(List<String> selectedCardIds);

    NarrativeGameViewData getNarrativeGameViewData();

    NarrativeGameViewData abandonGame();
}
