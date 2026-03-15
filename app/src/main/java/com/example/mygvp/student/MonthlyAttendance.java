package com.example.mygvp.student;

public class MonthlyAttendance {
    private String monthName;
    private int present;
    private int total;

    public MonthlyAttendance(String monthName, int present, int total) {
        this.monthName = monthName;
        this.present = present;
        this.total = total;
    }

    public String getMonthName() {
        String name = monthName;
        // Remove "1_" prefix used for sorting if present
        if (name != null && name.contains("_")) {
            name = name.substring(name.indexOf("_") + 1);
        }

        if (name == null || name.isEmpty()) return name;

        // Capitalize first letter, small rest
        if (name.length() == 1) return name.toUpperCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public int getPresent() { return present; }
    public int getTotal() { return total; }
    public String getPercentage() {
        if (total == 0) return "0%";
        return String.format("%.1f%%", (present * 100.0f / total));
    }
}