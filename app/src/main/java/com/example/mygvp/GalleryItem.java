package com.example.mygvp;

public class GalleryItem {
    private String title;
    private String imagePath; // Drawable name or URL

    public GalleryItem(String title, String imagePath) {
        this.title = title;
        this.imagePath = imagePath;
    }

    public String getTitle() { return title; }
    public String getImagePath() { return imagePath; }
}
