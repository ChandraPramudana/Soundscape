package com.chandra.soundscape;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class UserData {
    @SerializedName("id")
    private String userId;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("role")
    private String role;

    @SerializedName("member_since")
    private String memberSince;

    @SerializedName("profile_photo")
    private String profilePhoto;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("last_login")
    private String lastLogin;

    // Default constructor
    public UserData() {
    }

    // Constructor with basic parameters
    public UserData(String userId, String name, String email, String phone, String memberSince) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.memberSince = memberSince;
        this.role = "Listener"; // default role
    }

    // Constructor with role
    public UserData(String userId, String name, String email, String phone, String role, String memberSince) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.memberSince = memberSince;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getMemberSince() {
        return memberSince;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setMemberSince(String memberSince) {
        this.memberSince = memberSince;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    // Utility methods
    public boolean hasRole(String role) {
        return this.role != null && this.role.equals(role);
    }

    public boolean isAdmin() {
        return hasRole("Admin");
    }

    public boolean isListener() {
        return hasRole("Listener");
    }

    public boolean hasProfilePhoto() {
        return profilePhoto != null && !profilePhoto.isEmpty();
    }

    public boolean hasPhone() {
        return phone != null && !phone.isEmpty();
    }

    // Convert to UserModel
    public UserModel toUserModel() {
        return new UserModel(
                this.name != null ? this.name : "",
                this.email != null ? this.email : "",
                this.profilePhoto != null ? this.profilePhoto : ""
        );
    }

    // Get display name (name or email if name is empty)
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return email != null ? email : "User";
    }

    // Get formatted member since
    public String getFormattedMemberSince() {
        if (memberSince != null && !memberSince.isEmpty()) {
            return "Bergabung " + memberSince;
        }
        return "Bergabung baru-baru ini";
    }

    @Override
    public String toString() {
        return "UserData{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", memberSince='" + memberSince + '\'' +
                ", profilePhoto='" + profilePhoto + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", lastLogin='" + lastLogin + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserData userData = (UserData) obj;

        if (userId != null ? !userId.equals(userData.userId) : userData.userId != null)
            return false;
        return email != null ? email.equals(userData.email) : userData.email == null;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }
}