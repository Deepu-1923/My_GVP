package com.example.mygvp.student;

public class Achievement {
    private String id;
    private String rollNo;
    private String type;
    private String domain;
    private String fileUrl;
    private String date;

    public Achievement() {} // Required for Firebase

    public Achievement(String id, String rollNo, String type, String domain, String fileUrl, String date) {
        this.id = id;
        this.rollNo = rollNo;
        this.type = type;
        this.domain = domain;
        this.fileUrl = fileUrl;
        this.date = date;
    }

    public String getId() { return id; }
    public String getRollNo() { return rollNo; }
    public String getType() { return type; }
    public String getDomain() { return domain; }
    public String getFileUrl() { return fileUrl; }
    public String getDate() { return date; }
}
