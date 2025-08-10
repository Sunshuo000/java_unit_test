package com.coverage.analyzer;

import com.coverage.analyzer.models.CoverageResult;
import com.coverage.analyzer.models.ProjectStats;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.jacoco.agent.rt.IAgent;


public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -javaagent:jacocoagent.jar=output=none -jar coverage-analyzer.jar <project-path> [output-path] [ruleset-path]");
            System.exit(1);
        }

        Path projectPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);//args.length > 1 ? Paths.get(args[1]) : Paths.get(".");
        Path rulesetPath = args.length > 2 ? Paths.get(args[2]) : null;

        try {
            // 1. 首先加载Jacoco代理
            JacocoAgentLoader.loadAgent();

            // 验证代理状态
            IAgent agent = JacocoAgentLoader.getAgent();
            if (agent == null) {
                throw new RuntimeException("Jacoco agent failed to initialize");
            }
            System.out.println("Jacoco agent loaded successfully. Version: " + agent.getVersion());
            System.out.println("Session ID: " + agent.getSessionId());

            // 2. 解析项目结构
            ProjectParser parser = new ProjectParser(projectPath);
            ProjectStats stats = parser.parse();
            System.out.println("Project stats: " + stats.getNumJavaFiles() + " files, " +
                    stats.getNumTestMethods() + " tests");

            // 3. 运行测试收集覆盖率
            CoverageRunner runner = new CoverageRunner(projectPath, rulesetPath);
            runner.setTestTimeout(120); // 设置2分钟超时
            CoverageResult coverageResult = runner.collectCoverage();

            // 4. 导出结果
            ResultExporter exporter = new ResultExporter(outputPath);
            exporter.exportProjectStats(stats);
            exporter.exportCoverage(coverageResult);

            System.out.println("Analysis completed successfully!");
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}