package com.chandra.soundscape.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class JournalEvidence implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("music_id")
    private String musicId;

    @SerializedName("journal_id")
    private String journalId;

    @SerializedName("relevance_description")
    private String relevanceDescription;

    @SerializedName("evidence_strength")
    private String evidenceStrength;

    @SerializedName("created_at")
    private String createdAt;

    // Related objects
    @SerializedName("journal")
    private Journal journal;

    // Constructors
    public JournalEvidence() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMusicId() { return musicId; }
    public void setMusicId(String musicId) { this.musicId = musicId; }

    public String getJournalId() { return journalId; }
    public void setJournalId(String journalId) { this.journalId = journalId; }

    public String getRelevanceDescription() { return relevanceDescription; }
    public void setRelevanceDescription(String relevanceDescription) { this.relevanceDescription = relevanceDescription; }

    public String getEvidenceStrength() { return evidenceStrength; }
    public void setEvidenceStrength(String evidenceStrength) { this.evidenceStrength = evidenceStrength; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Journal getJournal() { return journal; }
    public void setJournal(Journal journal) { this.journal = journal; }
}
