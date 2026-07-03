package com.example.cardgame.model.narrative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NarrativeGameState {
    private List<Faction> factions = new ArrayList<>();
    private List<EventCard> cards = new ArrayList<>();
    private List<NarrativeNode> nodes = new ArrayList<>();
    private String userFactionId;
    private List<String> userHand = new ArrayList<>();
    private List<List<String>> userSequence = new ArrayList<>();
    private int userProgress;
    private int globalProgress;
    private int lastResolvedNodeIndex = -1;
    private Map<String, Integer> aiProgress = new HashMap<>();
    private double hearts = 3.0;
    private int correctCount;
    private int wrongCount;
    private int incompleteCount;
    private int severeErrorCount;
    private GameStatus status = GameStatus.PREVIEWING;

    public List<Faction> getFactions() {
        return factions;
    }

    public void setFactions(List<Faction> factions) {
        this.factions = factions != null ? new ArrayList<>(factions) : new ArrayList<Faction>();
    }

    public List<EventCard> getCards() {
        return cards;
    }

    public void setCards(List<EventCard> cards) {
        this.cards = cards != null ? new ArrayList<>(cards) : new ArrayList<EventCard>();
    }

    public List<NarrativeNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<NarrativeNode> nodes) {
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<NarrativeNode>();
    }

    public String getUserFactionId() {
        return userFactionId;
    }

    public void setUserFactionId(String userFactionId) {
        this.userFactionId = userFactionId;
    }

    public List<String> getUserHand() {
        return userHand;
    }

    public void setUserHand(List<String> userHand) {
        this.userHand = userHand != null ? new ArrayList<>(userHand) : new ArrayList<String>();
    }

    public List<List<String>> getUserSequence() {
        return userSequence;
    }

    public void setUserSequence(List<List<String>> userSequence) {
        this.userSequence = userSequence != null ? new ArrayList<>(userSequence) : new ArrayList<List<String>>();
    }

    public int getUserProgress() {
        return userProgress;
    }

    public void setUserProgress(int userProgress) {
        this.userProgress = userProgress;
    }

    public int getGlobalProgress() {
        return globalProgress;
    }

    public void setGlobalProgress(int globalProgress) {
        this.globalProgress = globalProgress;
    }

    public int getLastResolvedNodeIndex() {
        return lastResolvedNodeIndex;
    }

    public void setLastResolvedNodeIndex(int lastResolvedNodeIndex) {
        this.lastResolvedNodeIndex = lastResolvedNodeIndex;
    }

    public Map<String, Integer> getAiProgress() {
        return aiProgress;
    }

    public void setAiProgress(Map<String, Integer> aiProgress) {
        this.aiProgress = aiProgress != null ? new HashMap<>(aiProgress) : new HashMap<String, Integer>();
    }

    public double getHearts() {
        return hearts;
    }

    public void setHearts(double hearts) {
        this.hearts = hearts;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(int wrongCount) {
        this.wrongCount = wrongCount;
    }

    public int getIncompleteCount() {
        return incompleteCount;
    }

    public void setIncompleteCount(int incompleteCount) {
        this.incompleteCount = incompleteCount;
    }

    public int getSevereErrorCount() {
        return severeErrorCount;
    }

    public void setSevereErrorCount(int severeErrorCount) {
        this.severeErrorCount = severeErrorCount;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }
}
