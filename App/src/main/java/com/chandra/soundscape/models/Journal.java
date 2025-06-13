package com.chandra.soundscape.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Journal implements Parcelable {
    private String id;
    private String title;
    private String authors;
    private String journalName;
    private Integer publicationYear;
    private String doi;
    private String abstractSummary;
    private String studyType;
    private Integer sampleSize;
    private String keyFindings;
    private String evidenceGrade;
    private Integer qualityScore;
    private String createdAt;

    public Journal() {}

    protected Journal(Parcel in) {
        id = in.readString();
        title = in.readString();
        authors = in.readString();
        journalName = in.readString();
        if (in.readByte() == 0) {
            publicationYear = null;
        } else {
            publicationYear = in.readInt();
        }
        doi = in.readString();
        abstractSummary = in.readString();
        studyType = in.readString();
        if (in.readByte() == 0) {
            sampleSize = null;
        } else {
            sampleSize = in.readInt();
        }
        keyFindings = in.readString();
        evidenceGrade = in.readString();
        if (in.readByte() == 0) {
            qualityScore = null;
        } else {
            qualityScore = in.readInt();
        }
        createdAt = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(authors);
        dest.writeString(journalName);
        if (publicationYear == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(publicationYear);
        }
        dest.writeString(doi);
        dest.writeString(abstractSummary);
        dest.writeString(studyType);
        if (sampleSize == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(sampleSize);
        }
        dest.writeString(keyFindings);
        dest.writeString(evidenceGrade);
        if (qualityScore == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(qualityScore);
        }
        dest.writeString(createdAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Journal> CREATOR = new Creator<Journal>() {
        @Override
        public Journal createFromParcel(Parcel in) {
            return new Journal(in);
        }

        @Override
        public Journal[] newArray(int size) {
            return new Journal[size];
        }
    };

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getJournalName() { return journalName; }
    public void setJournalName(String journalName) { this.journalName = journalName; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }

    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }

    public String getAbstractSummary() { return abstractSummary; }
    public void setAbstractSummary(String abstractSummary) { this.abstractSummary = abstractSummary; }

    public String getStudyType() { return studyType; }
    public void setStudyType(String studyType) { this.studyType = studyType; }

    public Integer getSampleSize() { return sampleSize; }
    public void setSampleSize(Integer sampleSize) { this.sampleSize = sampleSize; }

    public String getKeyFindings() { return keyFindings; }
    public void setKeyFindings(String keyFindings) { this.keyFindings = keyFindings; }

    public String getEvidenceGrade() { return evidenceGrade; }
    public void setEvidenceGrade(String evidenceGrade) { this.evidenceGrade = evidenceGrade; }

    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}