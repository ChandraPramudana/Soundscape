package com.chandra.soundscape;

public class UserModel {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String profileImage;
    private String memberSince;

    // Default constructor
    public UserModel() {
        this.role = "Listener"; // default role
    }

    // Constructor with basic parameters
    public UserModel(String name, String email, String profileImage) {
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.role = "Listener"; // default role
    }

    // Constructor with all parameters
    public UserModel(String userId, String name, String email, String phone, String role, String profileImage, String memberSince) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role != null ? role : "Listener";
        this.profileImage = profileImage;
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

    public String getProfileImage() {
        return profileImage;
    }

    public String getMemberSince() {
        return memberSince;
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
        this.role = role != null ? role : "Listener";
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setMemberSince(String memberSince) {
        this.memberSince = memberSince;
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

    public boolean hasProfileImage() {
        return profileImage != null && !profileImage.isEmpty();
    }

    public boolean hasPhone() {
        return phone != null && !phone.isEmpty();
    }

    // Get display name (name or email if name is empty)
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return email != null ? email : "User";
    }

    // Get initials for avatar
    public String getInitials() {
        String displayName = getDisplayName();
        if (displayName.length() == 0) return "U";

        String[] parts = displayName.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else {
            return displayName.substring(0, Math.min(2, displayName.length())).toUpperCase();
        }
    }

    // Convert to UserData
    public UserData toUserData() {
        UserData userData = new UserData();
        userData.setUserId(this.userId);
        userData.setName(this.name);
        userData.setEmail(this.email);
        userData.setPhone(this.phone);
        userData.setRole(this.role);
        userData.setProfilePhoto(this.profileImage);
        userData.setMemberSince(this.memberSince);
        return userData;
    }

    // Static factory method from UserData
    public static UserModel fromUserData(UserData userData) {
        if (userData == null) return null;

        return new UserModel(
                userData.getUserId(),
                userData.getName(),
                userData.getEmail(),
                userData.getPhone(),
                userData.getRole(),
                userData.getProfilePhoto(),
                userData.getMemberSince()
        );
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", profileImage='" + profileImage + '\'' +
                ", memberSince='" + memberSince + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserModel userModel = (UserModel) obj;

        if (userId != null ? !userId.equals(userModel.userId) : userModel.userId != null)
            return false;
        return email != null ? email.equals(userModel.email) : userModel.email == null;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }
}