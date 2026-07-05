package com.example.cardgame.llm.narrative;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarrativePreCheckerTest {
    private final NarrativePreChecker preChecker = new NarrativePreChecker();

    @Test
    public void check_rejectsShortText() {
        assertFalse(preChecker.check("太短").isPassed());
    }

    @Test
    public void check_rejectsTextWithoutEventKeyword() {
        assertFalse(preChecker.check("甲方和乙方只是分别表达态度，没有任何清晰事件").isPassed());
    }

    @Test
    public void check_rejectsTextWithoutTwoActors() {
        assertFalse(preChecker.check("1911年，革命党发动起义并推动革命进程。").isPassed());
    }

    @Test
    public void check_acceptsTextWithTwoActorsAndEventKeyword() {
        assertTrue(preChecker.check("1911年，革命党发动起义，清朝政府随后组织镇压，双方围绕政权展开冲突。").isPassed());
    }
}
