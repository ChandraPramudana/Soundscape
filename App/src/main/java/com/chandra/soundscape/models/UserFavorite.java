package com.chandra.soundscape.models;

import android.os.Parcel;
import android.os.Parcelable;

public class UserFavorite implements Parcelable {
    private String id;
    private String userId;
    private String musicId;
    private String createdAt;

    public UserFavorite() {}

    protected UserFavorite(Parcel in) {
        id = in.readString();
        userId = in.readString();
        musicId = in.readString();
        createdAt = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeString(musicId);
        dest.writeString(createdAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserFavorite> CREATOR = new Creator<UserFavorite>() {
        @Override
        public UserFavorite createFromParcel(Parcel in) {
            return new UserFavorite(in);
        }

        @Override
        public UserFavorite[] newArray(int size) {
            return new UserFavorite[size];
        }
    };

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMusicId() { return musicId; }
    public void setMusicId(String musicId) { this.musicId = musicId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}