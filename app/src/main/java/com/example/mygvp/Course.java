package com.example.mygvp;

public class Course {
    private String name;
    private String fee;
    private String intake;

    public Course(String name, String fee, String intake) {
        this.name = name;
        this.fee = fee;
        this.intake = intake;
    }

    public String getName() { return name; }
    public String getFee() { return fee; }
    public String getIntake() { return intake; }
}
