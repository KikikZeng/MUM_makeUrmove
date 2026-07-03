package com.example.cardgame.controller.narrative;

import com.example.cardgame.dto.narrative.NarrativeGameViewData;
import com.example.cardgame.dto.narrative.NarrativePlayResult;
import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.dto.narrative.PreviewViewData;
import com.example.cardgame.model.narrative.GameStatus;
import com.example.cardgame.rule.narrative.NarrativePlayType;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NarrativeGameControllerTest {

    @Test
    public void parseText_buildsPreviewDataFromFallbackResult() {
        NarrativeGameController controller = new NarrativeGameController();

        ParseResult parseResult = controller.parseText("安史之乱");
        PreviewViewData preview = controller.getPreviewViewData();

        assertTrue(parseResult.isFallbackUsed());
        assertNotNull(preview.getGameId());
        assertEquals(4, preview.getFactions().size());
        assertEquals(6, preview.getTotalNodes());
        assertTrue(preview.getCardCountByFaction().get("tang") > 0);
    }

    @Test
    public void startNarrativeGame_returnsInitialViewDataForSelectedFaction() {
        NarrativeGameController controller = new NarrativeGameController();
        controller.parseText("安史之乱");

        NarrativeGameViewData viewData = controller.startNarrativeGame("tang");

        assertEquals("tang", viewData.getUserFactionId());
        assertEquals(0, viewData.getGlobalProgress());
        assertEquals(6, viewData.getTotalNodes());
        assertEquals(3.0, viewData.getHearts(), 0.0);
        assertFalse(viewData.getHandCards().isEmpty());
        assertNotNull(viewData.getStageTitle());
        assertNotNull(viewData.getOpeningNarration());
    }

    @Test
    public void startNarrativeGame_rejectsUnknownFaction() {
        NarrativeGameController controller = new NarrativeGameController();
        controller.parseText("安史之乱");

        NarrativeGameViewData viewData = controller.startNarrativeGame("missing");

        assertEquals("INVALID_FACTION", viewData.getMessage());
    }

    @Test
    public void submitEventCards_delegatesToEngineAndAdvancesViewData() {
        NarrativeGameController controller = new NarrativeGameController();
        controller.parseText("安史之乱");
        controller.startNarrativeGame("tang");

        List<String> currentNodeCards = controller.getNarrativeGameViewData()
                .getHandCards().isEmpty()
                ? java.util.Collections.<String>emptyList()
                : java.util.Collections.singletonList("t1");

        NarrativePlayResult result = controller.submitEventCards(currentNodeCards);
        NarrativeGameViewData viewData = controller.getNarrativeGameViewData();

        assertEquals(NarrativePlayType.PERFECT, result.getPlayType());
        assertEquals(1, viewData.getGlobalProgress());
        assertEquals(0, viewData.getLastResolvedNodeIndex());
        assertFalse(viewData.getLastResolvedNodeEvents().get("rebel").isEmpty());
    }

    @Test
    public void abandonGame_marksViewDataAbandoned() {
        NarrativeGameController controller = new NarrativeGameController();
        controller.parseText("安史之乱");
        controller.startNarrativeGame("tang");

        NarrativeGameViewData viewData = controller.abandonGame();

        assertEquals(GameStatus.ABANDONED, viewData.getStatus());
        assertTrue(viewData.isGameOver());
    }
}
