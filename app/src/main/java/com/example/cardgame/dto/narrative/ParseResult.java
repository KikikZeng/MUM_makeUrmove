package com.example.cardgame.dto.narrative;

import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.NarrativeNode;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FACTION_COUNT_INVALID = "FACTION_COUNT_INVALID";
    public static final String STATUS_PARSE_ERROR = "PARSE_ERROR";
    public static final String STATUS_MISSING_ACTION = "MISSING_ACTION";

    private List<Faction> factions = new ArrayList<>();
    private List<EventCard> cards = new ArrayList<>();
    private List<NarrativeNode> nodes = new ArrayList<>();
    private int totalNodes;
    private boolean fallbackUsed;
    private String parseStatus = STATUS_SUCCESS;
    private String errorMessage;

    public ParseResult() {
    }

    public ParseResult(List<Faction> factions, List<EventCard> cards, List<NarrativeNode> nodes,
                       int totalNodes, boolean fallbackUsed) {
        setFactions(factions);
        setCards(cards);
        setNodes(nodes);
        this.totalNodes = totalNodes;
        this.fallbackUsed = fallbackUsed;
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
        this.parseStatus = parseStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(parseStatus);
    }
}