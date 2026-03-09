package com.example.mygvp;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Faculty {
    private String name;
    private String email;
    private String qualification;
    private String specialization;
    private String imageUrl;

    public Faculty() {
        // Required empty constructor for Firebase
    }

    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getQualification() { return qualification; }
    public String getSpecialization() { return specialization; }
    public String getImageUrl() { return imageUrl; }

    // Setters (Required for Firebase to populate private fields)
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
