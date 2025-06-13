package com.chandra.soundscape;

public class Soundscape {
    private String id;
    private String title;
    private String creator;
    private String category;
    private String duration;
    private float rating;
    private int thumbnailResId;
    private boolean isFavorite;

    public Soundscape(String id, String title, String creator, String category,
                      String duration, float rating, int thumbnailResId) {
        this.id = id;
        this.title = title;
        this.creator = creator;
        this.category = category;
        this.duration = duration;
        this.rating = rating;
        this.thumbnailResId = thumbnailResId;
        this.isFavorite = false;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getThumbnailResId() { return thumbnailResId; }
    public void setThumbnailResId(int thumbnailResId) { this.thumbnailResId = thumbnailResId; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}

