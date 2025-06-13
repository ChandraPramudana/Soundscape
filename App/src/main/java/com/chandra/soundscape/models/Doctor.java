package com.chandra.soundscape.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Doctor implements Parcelable {
    private String id;
    private String name;
    private String specialization;
    private String credentials;
    private String photoUrl;
    private String bio;
    private int yearsOfExperience;
    private boolean isVerified;
    private String createdAt;
    private String updatedAt;

    public Doctor() {}

    protected Doctor(Parcel in) {
        id = in.readString();
        name = in.readString();
        specialization = in.readString();
        credentials = in.readString();
        photoUrl = in.readString();
        bio = in.readString();
        yearsOfExperience = in.readInt();
        isVerified = in.readByte() != 0;
        createdAt = in.readString();
        updatedAt = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(specialization);
        dest.writeString(credentials);
        dest.writeString(photoUrl);
        dest.writeString(bio);
        dest.writeInt(yearsOfExperience);
        dest.writeByte((byte) (isVerified ? 1 : 0));
        dest.writeString(createdAt);
        dest.writeString(updatedAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Doctor> CREATOR = new Creator<Doctor>() {
        @Override
        public Doctor createFromParcel(Parcel in) {
            return new Doctor(in);
        }

        @Override
        public Doctor[] newArray(int size) {
            return new Doctor[size];
        }
    };

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getCredentials() { return credentials; }
    public void setCredentials(String credentials) { this.credentials = credentials; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public int getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(int yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getFormattedName() {
        return name + (credentials != null ? ", " + credentials : "");
    }
}