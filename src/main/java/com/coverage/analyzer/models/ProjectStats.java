// ProjectStats.java
package com.coverage.analyzer.models;

public class ProjectStats {
    private String location;
    private int numJavaFiles;
    private int numClasses;
    private int numMethods;
    private int numTestMethods;

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
}