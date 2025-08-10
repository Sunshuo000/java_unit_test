package com.coverage.analyzer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.coverage.analyzer.models.ProjectStats;
import com.coverage.analyzer.models.CoverageResult;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ResultExporter {
    private final Path outputPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ResultExporter(Path outputPath) {
        this.outputPath = outputPath;
    }

    public void exportProjectStats(ProjectStats stats) throws IOException {
        Path filePath = outputPath.resolve("project_stats.json");
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            // 创建符合要求的JSON结构
            JsonObject root = new JsonObject();
            root.addProperty("location", stats.getLocation());

            JsonObject statsObj = new JsonObject();
            statsObj.addProperty("num_java_files", stats.getNumJavaFiles());
            statsObj.addProperty("num_classes", stats.getNumClasses());
            statsObj.addProperty("num_methods", stats.getNumMethods());
            statsObj.addProperty("num_test_methods", stats.getNumTestMethods());

            root.add("stat_of_repository", statsObj);

            gson.toJson(root, writer);
            System.out.println("Project stats saved to: " + filePath);
        }
    }

    public void exportCoverage(CoverageResult result) throws IOException {
        Path filePath = outputPath.resolve("test_coverage.json");
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            // 创建符合要求的JSON结构
            JsonObject root = new JsonObject();
            root.add("test_coverage_against_methods", gson.toJsonTree(result.getTestCoverageAgainstMethods()));
            gson.toJson(root, writer);
            System.out.println("Test coverage saved to: " + filePath);
        }
    }
}