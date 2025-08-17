package com.coverage.analyzer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.coverage.analyzer.models.ProjectStats;
import com.coverage.analyzer.models.CoverageResult;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ResultExporter {
    private final Path outputPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ResultExporter(Path outputPath) {
        this.outputPath = outputPath;
    }

    public void export(ProjectStats stats, CoverageResult coverageResult) throws IOException {
        Path filePath = outputPath.resolve("coverage_report.json");
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            JsonObject root = new JsonObject();
            root.addProperty("location", stats.getLocation());

            JsonObject statsObj = new JsonObject();
            statsObj.addProperty("num_java_files", stats.getNumJavaFiles());
            statsObj.addProperty("num_classes", stats.getNumClasses());
            statsObj.addProperty("num_methods", stats.getNumMethods());
            statsObj.addProperty("num_test_methods", stats.getNumTestMethods());
            root.add("stat_of_repository", statsObj);
            JsonObject coverageObj = new JsonObject();
            for (Map.Entry<String, List<String>> entry : coverageResult.getTestCoverageAgainstMethods().entrySet()) {
                coverageObj.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
            }
            root.add("test_coverage_against_methods", coverageObj);

            gson.toJson(root, writer);
            System.out.println("Coverage report saved to: " + filePath);
        }
    }
}