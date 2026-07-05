package com.example.cardgame.dto.narrative;

import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.NarrativeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreviewViewData {
    private String gameId;
    private List<Faction> factions = new ArrayList<>();
    private List<EventCard> cards = new ArrayList<>();
    private List<NarrativeNode> nodes = new ArrayList<>();
    private Map<String, Integer> cardCountByFaction = new HashMap<>();
    private String rawText;
    private int totalNodes;
    private boolean fallbackUsed;
    private String parseStatus = ParseResult.STATUS_SUCCESS;
    private String parseMessage = "";
    private boolean retryAllowed = true;
    private boolean requiresTextEdit;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

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

    public Map<String, Integer> getCardCountByFaction() {
        return cardCountByFaction;
    }

    public void setCardCountByFaction(Map<String, Integer> cardCountByFaction) {
        this.cardCountByFaction = cardCountByFaction != null
                ? new HashMap<>(cardCountByFaction)
                : new HashMap<String, Integer>();
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus != null ? parseStatus : ParseResult.STATUS_SUCCESS;
    }

    public String getParseMessage() {
        return parseMessage;
    }

    public void setParseMessage(String parseMessage) {
        this.parseMessage = parseMessage != null ? parseMessage : "";
    }

    public boolean isRetryAllowed() {
        return retryAllowed;
    }

    public void setRetryAllowed(boolean retryAllowed) {
        this.retryAllowed = retryAllowed;
    }

    public boolean isRequiresTextEdit() {
        return requiresTextEdit;
    }

    public void setRequiresTextEdit(boolean requiresTextEdit) {
        this.requiresTextEdit = requiresTextEdit;
    }
}
