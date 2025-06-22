package com.chandra.soundscape.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class MusicTrack implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private String artist;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("audio_url")
    private String audioUrl;

    @SerializedName("doctor_name")
    private String doctorName;

    @SerializedName("journal_reference")
    private String journalReference;

    @SerializedName("category")
    private String category;

    @SerializedName("duration")
    private String duration;

    @SerializedName("description")
    private String description;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("created_by")
    private String createdBy;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("play_count")
    private int playCount;

    @SerializedName("uploaded_by_doctor")
    private boolean uploadedByDoctor;

    @SerializedName("doctor_approval_status")
    private String doctorApprovalStatus;

    @SerializedName("doctor_id")
    private String doctorId;

    @SerializedName("is_doctor_recommendation")
    private boolean isDoctorRecommendation;

    @SerializedName("approval_status")
    private String approvalStatus;

    @SerializedName("approved_by")
    private String approvedBy;

    @SerializedName("approved_at")
    private String approvedAt;

    @SerializedName("rejection_reason")
    private String rejectionReason;

    // Constructor kosong
    public MusicTrack() {
        this.isActive = true;
        this.playCount = 0;
    }

    // Constructor dengan parameter dasar
    public MusicTrack(String title, String artist, String category) {
        this.title = title;
        this.artist = artist;
        this.category = category;
        this.isActive = true;
        this.playCount = 0;
    }

    // Constructor lengkap
    public MusicTrack(String title, String artist, String imageUrl, String audioUrl,
                      String doctorName, String journalReference, String category,
                      String duration, String description) {
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
        this.doctorName = doctorName;
        this.journalReference = journalReference;
        this.category = category;
        this.duration = duration;
        this.description = description;
        this.isActive = true;
        this.playCount = 0;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getImageUrl() { return imageUrl; }
    public String getAudioUrl() { return audioUrl; }
    public String getDoctorName() { return doctorName; }
    public String getJournalReference() { return journalReference; }
    public String getCategory() { return category; }
    public String getDuration() { return duration; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public boolean isActive() { return isActive; }
    public int getPlayCount() { return playCount; }

    public boolean isUploadedByDoctor() { return uploadedByDoctor; }
    public String getDoctorApprovalStatus() { return doctorApprovalStatus; }
    public String getDoctorId() { return doctorId; }
    public boolean isDoctorRecommendation() { return isDoctorRecommendation; }
    public String getApprovalStatus() { return approvalStatus; }
    public String getApprovedBy() { return approvedBy; }
    public String getApprovedAt() { return approvedAt; }
    public String getRejectionReason() { return rejectionReason; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public void setJournalReference(String journalReference) { this.journalReference = journalReference; }
    public void setCategory(String category) { this.category = category; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setActive(boolean active) { isActive = active; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }

    public void setUploadedByDoctor(boolean uploadedByDoctor) { this.uploadedByDoctor = uploadedByDoctor; }
    public void setDoctorApprovalStatus(String doctorApprovalStatus) { this.doctorApprovalStatus = doctorApprovalStatus; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public void setIsDoctorRecommendation(boolean isDoctorRecommendation) { this.isDoctorRecommendation = isDoctorRecommendation; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public void setApprovedAt(String approvedAt) { this.approvedAt = approvedAt; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    // Utility methods
    public boolean hasDoctor() {
        return doctorName != null && !doctorName.isEmpty();
    }

    public boolean hasJournal() {
        return journalReference != null && !journalReference.isEmpty();
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    public boolean hasAudio() {
        return audioUrl != null && !audioUrl.isEmpty();
    }

    public String getDisplayDuration() {
        return duration != null ? duration : "00:00";
    }

    public String getDisplayCategory() {
        return category != null ? category : "Uncategorized";
    }

    @Override
    public String toString() {
        return "MusicTrack{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", category='" + category + '\'' +
                ", playCount=" + playCount +
                '}';
    }

    // Tambahkan field untuk doctor recommendation
    @SerializedName("doctor_recommendation")
    private DoctorRecommendation doctorRecommendation;

    // Getter dan Setter
    public DoctorRecommendation getDoctorRecommendation() {
        return doctorRecommendation;
    }

    public void setDoctorRecommendation(DoctorRecommendation doctorRecommendation) {
        this.doctorRecommendation = doctorRecommendation;
    }

    public String getRecommendationText() {
        if (doctorRecommendation != null) {
            return doctorRecommendation.getRecommendationText();
        }
        return ""; // atau return null jika lebih sesuai
    }
}