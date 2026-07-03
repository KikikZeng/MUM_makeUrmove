package com.example.cardgame.dto.narrative;

import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.GameStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NarrativeGameViewData {
    private String userFactionId;
    private List<Faction> factions = new ArrayList<>();
    private List<EventCard> handCards = new ArrayList<>();
    private List<String> stageTitles = new ArrayList<>();
    private int globalProgress;
    private int totalNodes;
    private double hearts;
    private int correctCount;
    private int wrongCount;
    private int incompleteCount;
    private int severeErrorCount;
    private Map<String, Integer> aiProgress = new HashMap<>();
    private Map<String, List<EventCard>> currentNodeEvents = new HashMap<>();
    private Map<String, List<EventCard>> lastResolvedNodeEvents = new HashMap<>();
    private boolean gameOver;
    private GameStatus status;
    private String stageTitle;
    private String stageHint;
    private String openingNarration;
    private String resultNarration;
    private String message;
    private boolean currentUserNodeEmpty;
    private int lastResolvedNodeIndex = -1;

    public String getUserFactionId() {
        return userFactionId;
    }

    public void setUserFactionId(String userFactionId) {
        this.userFactionId = userFactionId;
    }

    public List<Faction> getFactions() {
        return factions;
    }

    public void setFactions(List<Faction> factions) {
        this.factions = factions != null ? new ArrayList<>(factions) : new ArrayList<Faction>();
    }

    public List<EventCard> getHandCards() {
        return handCards;
    }

    public void setHandCards(List<EventCard> handCards) {
        this.handCards = handCards != null ? new ArrayList<>(handCards) : new ArrayList<EventCard>();
    }

    public List<String> getStageTitles() {
        return stageTitles;
    }

    public void setStageTitles(List<String> stageTitles) {
        this.stageTitles = stageTitles != null ? new ArrayList<>(stageTitles) : new ArrayList<String>();
    }

    public int getGlobalProgress() {
        return globalProgress;
    }

    public void setGlobalProgress(int globalProgress) {
        this.globalProgress = globalProgress;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
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

    public Map<String, Integer> getAiProgress() {
        return aiProgress;
    }

    public void setAiProgress(Map<String, Integer> aiProgress) {
        this.aiProgress = aiProgress != null ? new HashMap<>(aiProgress) : new HashMap<String, Integer>();
    }

    public Map<String, List<EventCard>> getCurrentNodeEvents() {
        return currentNodeEvents;
    }

    public void setCurrentNodeEvents(Map<String, List<EventCard>> currentNodeEvents) {
        this.currentNodeEvents = currentNodeEvents != null
                ? new HashMap<>(currentNodeEvents)
                : new HashMap<String, List<EventCard>>();
    }

    public Map<String, List<EventCard>> getLastResolvedNodeEvents() {
        return lastResolvedNodeEvents;
    }

    public void setLastResolvedNodeEvents(Map<String, List<EventCard>> lastResolvedNodeEvents) {
        this.lastResolvedNodeEvents = lastResolvedNodeEvents != null
                ? new HashMap<>(lastResolvedNodeEvents)
                : new HashMap<String, List<EventCard>>();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCurrentUserNodeEmpty() {
        return currentUserNodeEmpty;
    }

    public void setCurrentUserNodeEmpty(boolean currentUserNodeEmpty) {
        this.currentUserNodeEmpty = currentUserNodeEmpty;
    }

    public int getLastResolvedNodeIndex() {
        return lastResolvedNodeIndex;
    }

    public void setLastResolvedNodeIndex(int lastResolvedNodeIndex) {
        this.lastResolvedNodeIndex = lastResolvedNodeIndex;
    }
}
