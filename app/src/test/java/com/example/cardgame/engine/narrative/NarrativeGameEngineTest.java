package com.example.cardgame.engine.narrative;

import com.example.cardgame.dto.narrative.NarrativePlayResult;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.GameStatus;
import com.example.cardgame.model.narrative.NarrativeGameState;
import com.example.cardgame.model.narrative.NarrativeNode;
import com.example.cardgame.rule.narrative.NarrativePlayType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarrativeGameEngineTest {

    @Test
    public void initializeGame_collectsOnlySelectedFactionCardsIntoUserHand() {
        NarrativeGameEngine engine = new NarrativeGameEngine();
        NarrativeGameState state = engine.initializeGame(factions(), cards(), nodes(), "tang");

        assertEquals(2, state.getUserHand().size());
        assertTrue(state.getUserHand().contains("t1"));
        assertTrue(state.getUserHand().contains("t2"));
        assertFalse(state.getUserHand().contains("r1"));
        assertEquals(0, state.getGlobalProgress());
        assertEquals(Integer.valueOf(0), state.getAiProgress().get("rebel"));
    }

    @Test
    public void perfectPlay_advancesAllFactionsAndGlobalProgress() {
        NarrativeGameEngine engine = new NarrativeGameEngine();
        engine.initializeGame(factions(), cards(), nodes(), "tang");

        NarrativePlayResult result = engine.playCards(Collections.singletonList("t1"));
        NarrativeGameState state = result.getGameState();

        assertEquals(NarrativePlayType.PERFECT, result.getPlayType());
        assertEquals(1, state.getGlobalProgress());
        assertEquals(1, state.getUserProgress());
        assertEquals(0, state.getLastResolvedNodeIndex());
        assertEquals(Integer.valueOf(1), state.getAiProgress().get("rebel"));
        assertEquals(GameStatus.PLAYING, state.getStatus());
    }

    @Test
    public void wrongPlay_doesNotAdvanceAnyProgress() {
        NarrativeGameEngine engine = new NarrativeGameEngine();
        engine.initializeGame(factions(), cards(), nodes(), "tang");

        NarrativePlayResult result = engine.playCards(Collections.singletonList("x1"));
        NarrativeGameState state = result.getGameState();

        assertEquals(NarrativePlayType.WRONG, result.getPlayType());
        assertEquals(0, state.getGlobalProgress());
        assertEquals(-1, state.getLastResolvedNodeIndex());
        assertEquals(0, state.getUserProgress());
        assertEquals(Integer.valueOf(0), state.getAiProgress().get("rebel"));
        assertEquals(2.0, state.getHearts(), 0.0);
    }

    @Test
    public void incompletePlay_removesAcceptedAndShatteredCardsThenAdvances() {
        NarrativeGameEngine engine = new NarrativeGameEngine();
        engine.initializeGame(factions(), cards(), nodesWithMultiCardUserNode(), "tang");

        NarrativePlayResult result = engine.playCards(Collections.singletonList("t1"));
        NarrativeGameState state = result.getGameState();

        assertEquals(NarrativePlayType.INCOMPLETE, result.getPlayType());
        assertFalse(state.getUserHand().contains("t1"));
        assertFalse(state.getUserHand().contains("t2"));
        assertEquals(1, state.getGlobalProgress());
        assertEquals(2.5, state.getHearts(), 0.0);
        assertEquals(1, state.getIncompleteCount());
    }

    @Test
    public void emptyUserNode_canContinueWithoutCardsAndStillAdvancesGlobalHistory() {
        NarrativeGameEngine engine = new NarrativeGameEngine();
        engine.initializeGame(factions(), cards(), nodesWithEmptyUserFirstNode(), "tang");

        NarrativePlayResult result = engine.playCards(Collections.<String>emptyList());
        NarrativeGameState state = result.getGameState();

        assertEquals(NarrativePlayType.PERFECT, result.getPlayType());
        assertEquals(1, state.getGlobalProgress());
        assertEquals(Integer.valueOf(1), state.getAiProgress().get("rebel"));
        assertEquals(3.0, state.getHearts(), 0.0);
    }

    @Test
    public void gameFinishesOnlyWhenGlobalProgressReachesNodeCount() {
        NarrativeGameEngine engine = new NarrativeGameEngine();
        engine.initializeGame(factions(), cards(), nodes(), "tang");

        engine.playCards(Collections.singletonList("t1"));
        assertEquals(GameStatus.PLAYING, engine.getGameState().getStatus());
        assertFalse(engine.isGameOver());

        engine.playCards(Collections.singletonList("t2"));
        assertEquals(GameStatus.FINISHED, engine.getGameState().getStatus());
        assertTrue(engine.isGameOver());
        assertEquals(2, engine.getGameState().getGlobalProgress());
    }

    @Test
    public void abandonGame_marksStateAbandoned() {
        NarrativeGameEngine engine = new NarrativeGameEngine();
        engine.initializeGame(factions(), cards(), nodes(), "tang");

        engine.abandonGame();

        assertEquals(GameStatus.ABANDONED, engine.getGameState().getStatus());
    }

    private List<Faction> factions() {
        return Arrays.asList(
                new Faction("tang", "唐廷", "中央朝廷"),
                new Faction("rebel", "叛军", "反叛势力")
        );
    }

    private List<EventCard> cards() {
        return Arrays.asList(
                new EventCard("t1", "tang", "调兵", "唐廷调兵", "第1段"),
                new EventCard("t2", "tang", "反攻", "唐廷反攻", "第2段"),
                new EventCard("r1", "rebel", "起兵", "叛军起兵", "第1段"),
                new EventCard("r2", "rebel", "扩张", "叛军扩张", "第2段")
        );
    }

    private List<NarrativeNode> nodes() {
        return Arrays.asList(
                node(0, "爆发", mapOf("tang", list("t1"), "rebel", list("r1"))),
                node(1, "转折", mapOf("tang", list("t2"), "rebel", list("r2")))
        );
    }

    private List<NarrativeNode> nodesWithMultiCardUserNode() {
        return Collections.singletonList(
                node(0, "爆发", mapOf("tang", list("t1", "t2"), "rebel", list("r1")))
        );
    }

    private List<NarrativeNode> nodesWithEmptyUserFirstNode() {
        return Arrays.asList(
                node(0, "爆发", mapOf("tang", list(), "rebel", list("r1"))),
                node(1, "转折", mapOf("tang", list("t2"), "rebel", list("r2")))
        );
    }

    private NarrativeNode node(int index, String title, Map<String, List<String>> factionCardIds) {
        return new NarrativeNode(index, title, "提示", "来源", factionCardIds);
    }

    private List<String> list(String... values) {
        return Arrays.asList(values);
    }

    private Map<String, List<String>> mapOf(String firstKey, List<String> firstValue,
                                            String secondKey, List<String> secondValue) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(firstKey, firstValue);
        map.put(secondKey, secondValue);
        return map;
    }
}
