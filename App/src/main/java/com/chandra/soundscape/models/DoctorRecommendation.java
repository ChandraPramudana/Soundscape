package com.chandra.soundscape.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DoctorRecommendation implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("music_id")
    private String musicId;

    @SerializedName("doctor_id")
    private String doctorId;

    @SerializedName("recommendation_text")
    private String recommendationText;

    @SerializedName("medical_rationale")
    private String medicalRationale;

    @SerializedName("dosage_instructions")
    private String dosageInstructions;

    @SerializedName("precautions")
    private String precautions;

    @SerializedName("confidence_level")
    private String confidenceLevel;

    @SerializedName("recommendation_strength")
    private String recommendationStrength;

    @SerializedName("created_at")
    private String createdAt;

    // Related objects
    @SerializedName("doctor")
    private Doctor doctor;

    // Constructors
    public DoctorRecommendation() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMusicId() { return musicId; }
    public void setMusicId(String musicId) { this.musicId = musicId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getRecommendationText() { return recommendationText; }
    public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }

    public String getMedicalRationale() { return medicalRationale; }
    public void setMedicalRationale(String medicalRationale) { this.medicalRationale = medicalRationale; }

    public String getDosageInstructions() { return dosageInstructions; }
    public void setDosageInstructions(String dosageInstructions) { this.dosageInstructions = dosageInstructions; }

    public String getPrecautions() { return precautions; }
    public void setPrecautions(String precautions) { this.precautions = precautions; }

    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }

    public String getRecommendationStrength() { return recommendationStrength; }
    public void setRecommendationStrength(String recommendationStrength) { this.recommendationStrength = recommendationStrength; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
}
