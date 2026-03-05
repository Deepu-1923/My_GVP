package com.example.mygvp;

public class ContactInfo {
    private String title;
    private String content;
    private int iconResId;

    public ContactInfo(String title, String content, int iconResId) {
        this.title = title;
        this.content = content;
        this.iconResId = iconResId;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getIconResId() { return iconResId; }
}