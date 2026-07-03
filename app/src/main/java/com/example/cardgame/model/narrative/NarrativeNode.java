package com.example.cardgame.model.narrative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NarrativeNode {
    private int nodeIndex;
    private String stageTitle;
    private String stageHint;
    private String sourceHint;
    private String openingNarration;
    private String resultNarration;
    private Map<String, List<String>> factionCardIds;

    public NarrativeNode() {
        this.factionCardIds = new HashMap<>();
    }

    public NarrativeNode(int nodeIndex, String stageTitle, String stageHint,
                         String sourceHint, Map<String, List<String>> factionCardIds) {
        this(nodeIndex, stageTitle, stageHint, sourceHint, null, null, factionCardIds);
    }

    public NarrativeNode(int nodeIndex, String stageTitle, String stageHint,
                         String sourceHint, String openingNarration, String resultNarration,
                         Map<String, List<String>> factionCardIds) {
        this.nodeIndex = nodeIndex;
        this.stageTitle = stageTitle;
        this.stageHint = stageHint;
        this.sourceHint = sourceHint;
        this.openingNarration = openingNarration;
        this.resultNarration = resultNarration;
        setFactionCardIds(factionCardIds);
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public String getStageTitle() {
        return stageTitle;
    }

    public void setStageTitle(String stageTitle) {
        this.stageTitle = stageTitle;
    }

    public String getStageHint() {
        return stageHint;
    }

    public void setStageHint(String stageHint) {
        this.stageHint = stageHint;
    }

    public String getSourceHint() {
        return sourceHint;
    }

    public void setSourceHint(String sourceHint) {
        this.sourceHint = sourceHint;
    }

    public String getOpeningNarration() {
        return openingNarration;
    }

    public void setOpeningNarration(String openingNarration) {
        this.openingNarration = openingNarration;
    }

    public String getResultNarration() {
        return resultNarration;
    }

    public void setResultNarration(String resultNarration) {
        this.resultNarration = resultNarration;
    }

    public Map<String, List<String>> getFactionCardIds() {
        return factionCardIds;
    }

    public void setFactionCardIds(Map<String, List<String>> factionCardIds) {
        this.factionCardIds = new HashMap<>();
        if (factionCardIds == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : factionCardIds.entrySet()) {
            List<String> cardIds = entry.getValue() != null
                    ? new ArrayList<>(entry.getValue())
                    : new ArrayList<>();
            this.factionCardIds.put(entry.getKey(), cardIds);
        }
    }

    public List<String> getCardIdsForFaction(String factionId) {
        List<String> cardIds = factionCardIds.get(factionId);
        return cardIds != null ? cardIds : new ArrayList<String>();
    }
}
