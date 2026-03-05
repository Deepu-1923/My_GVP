package com.example.mygvp;

public class ContactInfo {
    private String title;
    private String content;

    public ContactInfo(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
}