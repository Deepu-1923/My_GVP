package com.example.mygvp;

public class Faculty {
    private String name;
    private String designation;
    private String email;
    private String imagePath; // Local drawable name or URL

    public Faculty(String name, String designation, String email, String imagePath) {
        this.name = name;
        this.designation = designation;
        this.email = email;
        this.imagePath = imagePath;
    }

    public String getName() { return name; }
    public String getDesignation() { return designation; }
    public String getEmail() { return email; }
    public String getImagePath() { return imagePath; }
}
