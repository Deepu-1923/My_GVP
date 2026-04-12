package com.example.mygvp;

public class ChatOption {
    private String text;
    private int iconRes;

    public ChatOption(String text, int iconRes) {
        this.text = text;
        this.iconRes = iconRes;
    }

    public String getText() { return text; }
    public int getIconRes() { return iconRes; }
}
