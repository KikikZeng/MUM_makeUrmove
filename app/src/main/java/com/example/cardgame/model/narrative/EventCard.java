package com.example.cardgame.model.narrative;

public class EventCard {
    private String id;
    private String factionId;
    private String title;
    private String summary;
    private String eventTime;
    private String sourceHint;

    public EventCard() {
    }

    public EventCard(String id, String factionId, String title, String summary, String sourceHint) {
        this(id, factionId, title, summary, "时间不详", sourceHint);
    }

    public EventCard(String id, String factionId, String title, String summary,
                     String eventTime, String sourceHint) {
        this.id = id;
        this.factionId = factionId;
        this.title = title;
        this.summary = summary;
        this.eventTime = eventTime;
        this.sourceHint = sourceHint;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFactionId() {
        return factionId;
    }

    public void setFactionId(String factionId) {
        this.factionId = factionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getSourceHint() {
        return sourceHint;
    }

    public void setSourceHint(String sourceHint) {
        this.sourceHint = sourceHint;
    }
}
