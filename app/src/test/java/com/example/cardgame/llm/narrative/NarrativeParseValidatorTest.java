package com.example.cardgame.llm.narrative;

import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.engine.narrative.NarrativeGameEngine;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.GameStatus;
import com.example.cardgame.model.narrative.NarrativeNode;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarrativeParseValidatorTest {

    private final NarrativeParseValidator validator = new NarrativeParseValidator();

    @Test
    public void validateOrFallback_returnsFallbackWhenParseResultIsNull() {
        ParseResult result = validator.validateOrFallback(null);

        assertTrue(result.isFallbackUsed());
        assertEquals(4, result.getFactions().size());
        assertFalse(result.getNodes().isEmpty());
        assertFalse(result.getNodes().get(0).getOpeningNarration().isEmpty());
        assertFalse(result.getNodes().get(0).getResultNarration().isEmpty());
    }

    @Test
    public void validateOrFallback_normalizesValidResultAndFillsMissingFactionEntries() {
        ParseResult result = validator.validateOrFallback(validParseResultWithMissingFactionEntry());

        assertFalse(result.isFallbackUsed());
        assertEquals(1, result.getTotalNodes());
        assertTrue(result.getNodes().get(0).getFactionCardIds().containsKey("rebel"));
        assertTrue(result.getNodes().get(0).getCardIdsForFaction("rebel").isEmpty());
    }

    @Test
    public void validateOrFallback_returnsFallbackWhenNodeReferencesUnknownCard() {
        ParseResult parseResult = validParseResultWithMissingFactionEntry();
        parseResult.getNodes().get(0).getFactionCardIds().get("tang").add("missing");

        ParseResult result = validator.validateOrFallback(parseResult);

        assertTrue(result.isFallbackUsed());
    }

    @Test
    public void validateOrFallback_returnsFallbackWhenCardFactionDoesNotMatchNodeFaction() {
        ParseResult parseResult = validParseResultWithMissingFactionEntry();
        parseResult.getNodes().get(0).getFactionCardIds().put("rebel", Collections.singletonList("t1"));

        ParseResult result = validator.validateOrFallback(parseResult);

        assertTrue(result.isFallbackUsed());
    }

    @Test
    public void fallbackData_canInitializeAndFinishNarrativeGame() {
        ParseResult fallback = new FallbackNarrativeDataProvider().getFallbackData();
        NarrativeGameEngine engine = new NarrativeGameEngine();
        engine.initializeGame(fallback.getFactions(), fallback.getCards(), fallback.getNodes(), "tang");

        while (!engine.isGameOver()) {
            int progress = engine.getGameState().getGlobalProgress();
            List<String> tangCards = fallback.getNodes().get(progress).getCardIdsForFaction("tang");
            engine.playCards(tangCards);
        }

        assertEquals(GameStatus.FINISHED, engine.getGameState().getStatus());
        assertEquals(fallback.getTotalNodes(), engine.getGameState().getGlobalProgress());
    }

    private ParseResult validParseResultWithMissingFactionEntry() {
        List<Faction> factions = Arrays.asList(
                new Faction("tang", "唐廷", "中央朝廷"),
                new Faction("rebel", "叛军", "反叛势力")
        );
        List<EventCard> cards = Collections.singletonList(
                new EventCard("t1", "tang", "调兵", "唐廷调兵", "第1段")
        );
        List<NarrativeNode> nodes = Collections.singletonList(
                new NarrativeNode(0, "起势", "局势开始变化", "第1段", mapOf("tang", list("t1")))
        );
        return new ParseResult(factions, cards, nodes, 0, true);
    }

    private List<String> list(String... values) {
        return Arrays.asList(values);
    }

    private Map<String, List<String>> mapOf(String key, List<String> value) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
