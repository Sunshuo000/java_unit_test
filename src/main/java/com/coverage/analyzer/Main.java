package com.coverage.analyzer;

import com.coverage.analyzer.models.CoverageResult;
import com.coverage.analyzer.models.ProjectStats;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -javaagent:jacocoagent.jar=output=none -jar coverage-analyzer.jar <project-path> [output-path] [ruleset-path]");
            System.exit(1);
        }

        Path projectPath = Paths.get(args[0]);
        Path outputPath = args.length > 1 ? Paths.get(args[1]) : Paths.get(".");
        Path rulesetPath = args.length > 2 ? Paths.get(args[2]) : null;

        try {
            // 1. 加载Jacoco代理
            JacocoAgentLoader.loadAgent();

            // 2. 解析项目结构
            ProjectParser parser = new ProjectParser(projectPath);
            ProjectStats stats = parser.parse();

            // 设置报告中的测试方法数量
            int reportedTestCount = (stats.getNumTestMethods());
            stats.setReportedTestMethods(reportedTestCount);

            System.out.println("Project stats: " + stats.getNumJavaFiles() + " files, " +
                    reportedTestCount + " tests");

            // 3. 运行测试收集覆盖率
            long startTime = System.currentTimeMillis();
            CoverageRunner runner = new CoverageRunner(projectPath, rulesetPath);
            runner.setTestTimeout(120);
            CoverageResult coverageResult = runner.collectCoverage();
            long endTime = System.currentTimeMillis();
            long durationSec = (endTime - startTime) / 1000;

            // 4. 导出合并结果
            ResultExporter exporter = new ResultExporter(outputPath);
            exporter.export(projectPath, stats, coverageResult);

            System.out.println("Analysis completed successfully!");
            System.out.println("执行耗时（秒）: " + durationSec);
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            System.exit(1);
        }
    }
}