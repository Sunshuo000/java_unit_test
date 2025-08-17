package com.coverage.analyzer.models;

public class ProjectStats {
    private String location;
    private int numJavaFiles;
    private int numClasses;
    private int numMethods;
    private int numTestMethods;
    private int reportedTestMethods; // 新增字段

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getNumJavaFiles() { return numJavaFiles; }
    public void incrementJavaFiles() { numJavaFiles++; }

    public int getNumClasses() { return numClasses; }
    public void incrementClasses() { numClasses++; }

    public int getNumMethods() { return numMethods; }
    public void incrementMethods() { numMethods++; }

    public int getNumTestMethods() { return numTestMethods; }
    public void setNumTestMethods(int numTestMethods) { this.numTestMethods = numTestMethods; }

    // 新增getter/setter
    public int getReportedTestMethods() { return reportedTestMethods; }
    public void setReportedTestMethods(int reportedTestMethods) {
        this.reportedTestMethods = reportedTestMethods;
    }
}