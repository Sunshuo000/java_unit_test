package com.coverage.analyzer;

import com.coverage.analyzer.models.CoverageResult;
import com.coverage.analyzer.models.ProjectStats;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar coverage-analyzer.jar <project-path> [output-path] [ruleset-path]");
            System.exit(1);
        }

        Path projectPath = Paths.get(args[0]);
        Path outputPath = args.length > 1 ? Paths.get(args[1]) : Paths.get(".");
        Path rulesetPath = args.length > 2 ? Paths.get(args[2]) : null;

        try {
            // 1. 首先加载Jacoco代理

            // 2. 解析项目结构


            // 3. 初始化方法映射器

            // 4. 运行测试收集覆盖率

            // 5. 导出结果

        } catch (Exception e) {
            System.exit(1);
        }
    }
}