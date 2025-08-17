package com.coverage.analyzer;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public class CoverageAnalyzer {
    private final ExecutionDataStore executionDataStore;
    private final Path classesDir;

    public CoverageAnalyzer(ExecutionDataStore executionDataStore, Path classesDir) {
        this.executionDataStore = executionDataStore;
        this.classesDir = classesDir;
    }

    public Collection<IClassCoverage> analyze() throws IOException {
        // 创建覆盖率构建器
        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        // 添加分析范围
        if (classesDir != null && Files.exists(classesDir)) {
            File sourceDir = classesDir.toFile();
            if (sourceDir.isDirectory()) {
                System.out.println("Analyzing classes in: " + classesDir);
                analyzer.analyzeAll(sourceDir);
            } else {
                System.out.println("Classes directory is not a directory: " + classesDir);
            }
        } else {
            System.out.println("Classes directory not found: " + classesDir);
        }

        // 返回所有分析过的类
        return coverageBuilder.getClasses();
    }
}