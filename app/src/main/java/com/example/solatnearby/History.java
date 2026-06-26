package com.example.solatnearby;

public class History {
    private String id;
    private String userId;
    private String masjidName;
    private String masjidAddress;
    private double masjidLat;
    private double masjidLng;
    private String date;
    private String time;
    private String userNote;
    private boolean isFavorite;

    public History() {
    }

    public History(String userId, String masjidName, String masjidAddress,
                   double masjidLat, double masjidLng, String date, String time,
                   String userNote, boolean isFavorite) {
        this.userId = userId;
        this.masjidName = masjidName;
        this.masjidAddress = masjidAddress;
        this.masjidLat = masjidLat;
        this.masjidLng = masjidLng;
        this.date = date;
        this.time = time;
        this.userNote = userNote;
        this.isFavorite = isFavorite;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMasjidName() {
        return masjidName;
    }

    public void setMasjidName(String masjidName) {
        this.masjidName = masjidName;
    }

    public String getMasjidAddress() {
        return masjidAddress;
    }

    public void setMasjidAddress(String masjidAddress) {
        this.masjidAddress = masjidAddress;
    }

    public double getMasjidLat() {
        return masjidLat;
    }

    public void setMasjidLat(double masjidLat) {
        this.masjidLat = masjidLat;
    }

    public double getMasjidLng() {
        return masjidLng;
    }

    public void setMasjidLng(double masjidLng) {
        this.masjidLng = masjidLng;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUserNote() {
        return userNote;
    }

    public void setUserNote(String userNote) {
        this.userNote = userNote;
    }



    public boolean getFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

}