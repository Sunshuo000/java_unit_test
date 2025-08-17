package com.coverage.analyzer.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoverageResult {
    private Map<String, List<String>> testCoverageAgainstMethods = new HashMap<>();
    public void addCoverage(String testMethod, List<String> coveredMethods) {
        testCoverageAgainstMethods.put(testMethod, coveredMethods);
    }

    public Map<String, List<String>> getTestCoverageAgainstMethods() {
        return testCoverageAgainstMethods;
    }
}