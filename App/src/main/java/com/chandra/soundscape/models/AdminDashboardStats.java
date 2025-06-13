package com.chandra.soundscape.models;

import android.os.Parcel;
import android.os.Parcelable;

public class AdminDashboardStats implements Parcelable {
    private String id;
    private String statDate;
    private int totalUsers;
    private int totalMusicTracks;
    private int totalPlaysToday;
    private int totalDownloadsToday;
    private int newUsersToday;
    private String mostPlayedMusicId;
    private String updatedAt;

    public AdminDashboardStats() {}

    protected AdminDashboardStats(Parcel in) {
        id = in.readString();
        statDate = in.readString();
        totalUsers = in.readInt();
        totalMusicTracks = in.readInt();
        totalPlaysToday = in.readInt();
        totalDownloadsToday = in.readInt();
        newUsersToday = in.readInt();
        mostPlayedMusicId = in.readString();
        updatedAt = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(statDate);
        dest.writeInt(totalUsers);
        dest.writeInt(totalMusicTracks);
        dest.writeInt(totalPlaysToday);
        dest.writeInt(totalDownloadsToday);
        dest.writeInt(newUsersToday);
        dest.writeString(mostPlayedMusicId);
        dest.writeString(updatedAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AdminDashboardStats> CREATOR = new Creator<AdminDashboardStats>() {
        @Override
        public AdminDashboardStats createFromParcel(Parcel in) {
            return new AdminDashboardStats(in);
        }

        @Override
        public AdminDashboardStats[] newArray(int size) {
            return new AdminDashboardStats[size];
        }
    };

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatDate() { return statDate; }
    public void setStatDate(String statDate) { this.statDate = statDate; }

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public int getTotalMusicTracks() { return totalMusicTracks; }
    public void setTotalMusicTracks(int totalMusicTracks) { this.totalMusicTracks = totalMusicTracks; }

    public int getTotalPlaysToday() { return totalPlaysToday; }
    public void setTotalPlaysToday(int totalPlaysToday) { this.totalPlaysToday = totalPlaysToday; }

    public int getTotalDownloadsToday() { return totalDownloadsToday; }
    public void setTotalDownloadsToday(int totalDownloadsToday) { this.totalDownloadsToday = totalDownloadsToday; }

    public int getNewUsersToday() { return newUsersToday; }
    public void setNewUsersToday(int newUsersToday) { this.newUsersToday = newUsersToday; }

    public String getMostPlayedMusicId() { return mostPlayedMusicId; }
    public void setMostPlayedMusicId(String mostPlayedMusicId) { this.mostPlayedMusicId = mostPlayedMusicId; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}