package com.example.mygvp.admin;

public class StudentFee {
    private String rollNumber;
    private String name;
    private double totalFee;
    private double paidAmount;
    private double dueAmount;

    public StudentFee() {
    }

    public StudentFee(String rollNumber, String name, double totalFee, double paidAmount, double dueAmount) {
        this.rollNumber = rollNumber;
        this.name = name;
        this.totalFee = totalFee;
        this.paidAmount = paidAmount;
        this.dueAmount = dueAmount;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(double totalFee) {
        this.totalFee = totalFee;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public double getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(double dueAmount) {
        this.dueAmount = dueAmount;
    }
}
