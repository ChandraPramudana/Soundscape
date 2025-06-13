package com.chandra.soundscape.models;

import java.io.Serializable;

public class MusicCategory implements Serializable {
    private String id;
    private String name;
    private String description;
    private int iconResId;
    private int itemCount;

    // Constructors
    public MusicCategory() {
    }

    public MusicCategory(String id, String name, int iconResId) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
    }

    public MusicCategory(String id, String name, String description, int iconResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconResId = iconResId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    @Override
    public String toString() {
        return "MusicCategory{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", itemCount=" + itemCount +
                '}';
    }
}