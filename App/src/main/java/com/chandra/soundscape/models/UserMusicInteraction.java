package com.chandra.soundscape.models;

import android.os.Parcel;
import android.os.Parcelable;

public class UserMusicInteraction implements Parcelable {
    private String id;
    private String userId;
    private String musicId;
    private String interactionType; // 'play', 'favorite', 'download', 'rating'
    private Integer rating;
    private Integer listeningDurationSeconds;
    private String moodBefore;
    private String moodAfter;
    private String feedback;
    private String createdAt;

    public UserMusicInteraction() {}

    protected UserMusicInteraction(Parcel in) {
        id = in.readString();
        userId = in.readString();
        musicId = in.readString();
        interactionType = in.readString();
        if (in.readByte() == 0) {
            rating = null;
        } else {
            rating = in.readInt();
        }
        if (in.readByte() == 0) {
            listeningDurationSeconds = null;
        } else {
            listeningDurationSeconds = in.readInt();
        }
        moodBefore = in.readString();
        moodAfter = in.readString();
        feedback = in.readString();
        createdAt = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeString(musicId);
        dest.writeString(interactionType);
        if (rating == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(rating);
        }
        if (listeningDurationSeconds == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(listeningDurationSeconds);
        }
        dest.writeString(moodBefore);
        dest.writeString(moodAfter);
        dest.writeString(feedback);
        dest.writeString(createdAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserMusicInteraction> CREATOR = new Creator<UserMusicInteraction>() {
        @Override
        public UserMusicInteraction createFromParcel(Parcel in) {
            return new UserMusicInteraction(in);
        }

        @Override
        public UserMusicInteraction[] newArray(int size) {
            return new UserMusicInteraction[size];
        }
    };

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMusicId() { return musicId; }
    public void setMusicId(String musicId) { this.musicId = musicId; }

    public String getInteractionType() { return interactionType; }
    public void setInteractionType(String interactionType) { this.interactionType = interactionType; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Integer getListeningDurationSeconds() { return listeningDurationSeconds; }
    public void setListeningDurationSeconds(Integer listeningDurationSeconds) { this.listeningDurationSeconds = listeningDurationSeconds; }

    public String getMoodBefore() { return moodBefore; }
    public void setMoodBefore(String moodBefore) { this.moodBefore = moodBefore; }

    public String getMoodAfter() { return moodAfter; }
    public void setMoodAfter(String moodAfter) { this.moodAfter = moodAfter; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}