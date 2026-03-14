package com.example.mygvp.student;

public class Achievement {
    private String id;
    private String uploaderId; // rollNo for students, facultyId for faculty
    private String uploaderName;
    private String uploaderType; // "STUDENT" or "FACULTY"
    private String type; // Hackathon, Certification, etc.
    private String domain;
    private String fileUrl;
    private String date;
    private String courseName; // Extracted via OCR

    public Achievement() {} // Required for Firebase

    public Achievement(String id, String uploaderId, String uploaderName, String uploaderType, String type, String domain, String fileUrl, String date, String courseName) {
        this.id = id;
        this.uploaderId = uploaderId;
        this.uploaderName = uploaderName;
        this.uploaderType = uploaderType;
        this.type = type;
        this.domain = domain;
        this.fileUrl = fileUrl;
        this.date = date;
        this.courseName = courseName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUploaderId() { return uploaderId; }
    public void setUploaderId(String uploaderId) { this.uploaderId = uploaderId; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
    public String getUploaderType() { return uploaderType; }
    public void setUploaderType(String uploaderType) { this.uploaderType = uploaderType; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
}
