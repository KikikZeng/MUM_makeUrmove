package com.example.cardgame.dto.narrative;

import com.example.cardgame.model.narrative.EventCard;
import com.example.cardgame.model.narrative.Faction;
import com.example.cardgame.model.narrative.NarrativeNode;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {
    private List<Faction> factions = new ArrayList<>();
    private List<EventCard> cards = new ArrayList<>();
    private List<NarrativeNode> nodes = new ArrayList<>();
    private int totalNodes;
    private boolean fallbackUsed;

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
}
