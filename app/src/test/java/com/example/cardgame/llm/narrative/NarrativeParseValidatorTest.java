package com.example.cardgame.llm.narrative;

import com.example.cardgame.dto.narrative.ParseResult;
import com.example.cardgame.engine.narrative.NarrativeGameEngine;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.GameStatus;
import com.example.cardgame.model.narrative.NarrativeNode;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
        assertEquals(2, result.getTotalNodes());
        assertTrue(result.getNodes().get(0).getFactionCardIds().containsKey("rebel"));
        assertTrue(result.getNodes().get(0).getCardIdsForFaction("rebel").isEmpty());
    }

    @Test
    public void validateOrFallback_returnsFallbackWhenFactionHasNoAssignedCards() {
        ParseResult parseResult = validParseResultWithMissingFactionEntry();
        parseResult.getNodes().remove(1);

        ParseResult result = validator.validateOrFallback(parseResult);

        assertTrue(result.isFallbackUsed());
    }

    @Test
    public void validateOrFallback_returnsFallbackWhenFactionIsNeverAssigned() {
        ParseResult parseResult = validParseResultWithMissingFactionEntry();
        parseResult.getFactions().add(new Faction("local", "地方势力", "事件不足的地方阵营"));
        parseResult.getCards().add(new EventCard("l1", "local", "地方观望", "地方保持观望", "地方态度"));

        ParseResult result = validator.validateOrFallback(parseResult);

        assertTrue(result.isFallbackUsed());
    }

    @Test
    public void validateOrFallback_repairsRepeatedCardDisplayTextWithoutFallback() {
        ParseResult parseResult = validParseResultWithMissingFactionEntry();
        EventCard card = parseResult.getCards().get(0);
        card.setTitle("清帝退位");
        card.setSummary("清帝退位");
        card.setSourceHint("清帝退位");

        ParseResult result = validator.validateOrFallback(parseResult);
        EventCard repairedCard = result.getCards().get(0);

        assertFalse(result.isFallbackUsed());
        assertNotEquals(repairedCard.getTitle(), repairedCard.getSummary());
        assertNotEquals(repairedCard.getTitle(), repairedCard.getSourceHint());
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
        List<Faction> factions = new ArrayList<>(Arrays.asList(
                new Faction("tang", "唐廷", "中央朝廷"),
                new Faction("rebel", "叛军", "反叛势力")
        ));
        List<EventCard> cards = new ArrayList<>(Arrays.asList(
                new EventCard("t1", "tang", "调兵", "唐廷调兵", "第1段"),
                new EventCard("r1", "rebel", "起兵", "叛军起兵", "第2段")
        ));
        List<NarrativeNode> nodes = new ArrayList<>(Arrays.asList(
                new NarrativeNode(0, "起势", "局势开始变化", "第1段", mapOf("tang", list("t1"))),
                new NarrativeNode(1, "反叛", "反叛势力行动", "第2段", mapOf("rebel", list("r1")))
        ));
        return new ParseResult(factions, cards, nodes, 2, false);
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
