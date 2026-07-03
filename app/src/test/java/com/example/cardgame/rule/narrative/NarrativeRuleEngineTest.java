package com.example.cardgame.rule.narrative;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NarrativeRuleEngineTest {

    private final NarrativeRuleEngine ruleEngine = new NarrativeRuleEngine();

    @Test
    public void validatePlay_returnsPerfectWhenSelectionMatchesCurrentNode() {
        NarrativeValidationResult result = ruleEngine.validatePlay(
                Arrays.asList("c1", "c2"),
                Arrays.asList("c1", "c2")
        );

        assertEquals(NarrativePlayType.PERFECT, result.getPlayType());
        assertTrue(result.getShatteredCardIds().isEmpty());
        assertEquals(2, result.getAcceptedCardIds().size());
    }

    @Test
    public void validatePlay_returnsIncompleteWhenSelectionIsNonEmptySubset() {
        NarrativeValidationResult result = ruleEngine.validatePlay(
                Collections.singletonList("c1"),
                Arrays.asList("c1", "c2")
        );

        assertEquals(NarrativePlayType.INCOMPLETE, result.getPlayType());
        assertEquals(Collections.singletonList("c1"), result.getAcceptedCardIds());
        assertEquals(Collections.singletonList("c2"), result.getShatteredCardIds());
    }

    @Test
    public void validatePlay_returnsWrongWhenSelectionHasNoIntersection() {
        NarrativeValidationResult result = ruleEngine.validatePlay(
                Collections.singletonList("x1"),
                Arrays.asList("c1", "c2")
        );

        assertEquals(NarrativePlayType.WRONG, result.getPlayType());
        assertTrue(result.getAcceptedCardIds().isEmpty());
    }

    @Test
    public void validatePlay_returnsWrongWhenSelectionMixesCorrectAndWrongCards() {
        NarrativeValidationResult result = ruleEngine.validatePlay(
                Arrays.asList("c1", "x1"),
                Arrays.asList("c1", "c2")
        );

        assertEquals(NarrativePlayType.WRONG, result.getPlayType());
        assertTrue(result.getAcceptedCardIds().isEmpty());
    }

    @Test
    public void validatePlay_returnsWrongWhenSelectionIsEmptyForNonEmptyNode() {
        NarrativeValidationResult result = ruleEngine.validatePlay(
                Collections.<String>emptyList(),
                Collections.singletonList("c1")
        );

        assertEquals(NarrativePlayType.WRONG, result.getPlayType());
    }

    @Test
    public void validatePlay_returnsPerfectWhenEmptyNodeContinuesWithoutCards() {
        NarrativeValidationResult result = ruleEngine.validatePlay(
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
        );

        assertEquals(NarrativePlayType.PERFECT, result.getPlayType());
        assertTrue(result.getAcceptedCardIds().isEmpty());
    }

    @Test
    public void validatePlay_returnsWrongWhenEmptyNodeReceivesCards() {
        NarrativeValidationResult result = ruleEngine.validatePlay(
                Collections.singletonList("c1"),
                Collections.<String>emptyList()
        );

        assertEquals(NarrativePlayType.WRONG, result.getPlayType());
    }
}
