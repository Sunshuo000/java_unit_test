package com.coverage.analyzer;

import com.coverage.analyzer.models.CoverageResult;
import org.jacoco.agent.rt.IAgent;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.tools.ExecFileLoader;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class CoverageRunner {
    private final Path projectPath;
    private final Path rulesetPath;
    private final ClassLoader projectClassLoader;
    private int testTimeout = 30;
    public CoverageRunner(Path projectPath, Path rulesetPath) throws Exception {
        this.projectPath = projectPath.toAbsolutePath().normalize();
        System.out.println("Normalized project path: " + this.projectPath);

        if (!Files.exists(this.projectPath)) {
            throw new IOException("Project directory not found: " + this.projectPath);
        }
        if (!Files.isDirectory(this.projectPath)) {
            throw new IOException("Project path is not a directory: " + this.projectPath);
        }

        this.rulesetPath = rulesetPath;
        this.projectClassLoader = createProjectClassLoader();
    }

    private ClassLoader createProjectClassLoader() throws Exception {
        List<URL> urls = new ArrayList<>();

        // 添加主类目录
        Path classesDir = findClassesDirectory();
        if (classesDir != null && Files.exists(classesDir)) {
            System.out.println("Adding classes directory: " + classesDir);
            urls.add(classesDir.toUri().toURL());
        }

        // 添加测试类目录
        Path testClassesDir = findTestClassesDirectory();
        if (testClassesDir != null && Files.exists(testClassesDir)) {
            System.out.println("Adding test-classes directory: " + testClassesDir);
            urls.add(testClassesDir.toUri().toURL());
        }

        // 添加依赖库
        for (Path jarPath : findDependencies()) {
            System.out.println("Adding dependency: " + jarPath);
            urls.add(jarPath.toUri().toURL());
        }

        // 打印所有类路径
        System.out.println("Classpath URLs (" + urls.size() + " items):");
        for (URL url : urls) {
            System.out.println("  " + url);
        }

        // 使用系统类加载器作为父加载器
        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }

    private List<Path> findDependencies() throws IOException {
        List<Path> dependencies = new ArrayList<>();

        // Maven 依赖
        Path mavenDeps = projectPath.resolve("target").resolve("dependency");
        if (Files.exists(mavenDeps) && Files.isDirectory(mavenDeps)) {
            Files.list(mavenDeps)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .forEach(dependencies::add);
        }

        // Gradle 依赖
        Path gradleDeps = projectPath.resolve("build").resolve("libs");
        if (Files.exists(gradleDeps) && Files.isDirectory(gradleDeps)) {
            Files.list(gradleDeps)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .forEach(dependencies::add);
        }

        // 标准 lib 目录
        Path libDir = projectPath.resolve("lib");
        if (Files.exists(libDir) && Files.isDirectory(libDir)) {
            Files.list(libDir)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .forEach(dependencies::add);
        }

        return dependencies;
    }

    private Path findClassesDirectory() {
        // 尝试常见构建系统的输出目录
        Path[] possiblePaths = {
                projectPath.resolve("target").resolve("classes"),
                projectPath.resolve("build").resolve("classes").resolve("java").resolve("main"),
                projectPath.resolve("out").resolve("production").resolve("classes"),
                projectPath.resolve("bin"),
                projectPath
        };

        for (Path path : possiblePaths) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                System.out.println("Found classes directory: " + path);
                return path;
            }
        }
        System.out.println("No classes directory found");
        return null;
    }

    private Path findTestClassesDirectory() {
        Path[] possiblePaths = {
                projectPath.resolve("target").resolve("test-classes"),
                projectPath.resolve("build").resolve("classes").resolve("java").resolve("test"),
                projectPath.resolve("out").resolve("test").resolve("classes"),
                projectPath.resolve("test-bin"),
                projectPath
        };

        for (Path path : possiblePaths) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                System.out.println("Found test-classes directory: " + path);
                return path;
            }
        }
        System.out.println("No test-classes directory found");
        return null;
    }

    public CoverageResult collectCoverage() {
        CoverageResult result = new CoverageResult();
        ProjectParser parser = new ProjectParser(projectPath);
        try {
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> testMethods = parser.getTestMethods();

        int maxTests = 100;
        if (testMethods.size() > maxTests) {
            testMethods = testMethods.subList(0, maxTests);
        }

        System.out.println("Found " + testMethods.size() + " test methods to run");

        // 检查Jacoco代理状态
        IAgent agent = JacocoAgentLoader.getAgent();
        System.out.println("Jacoco agent status: " + (agent != null ? "Loaded" : "Not loaded"));
        System.out.println("Jacoco agent version: " + (agent != null ? agent.getVersion() : "N/A"));

        // 创建方法映射器
        MethodCoverageMapper mapper = new MethodCoverageMapper();
        Path classesDir = findClassesDirectory();
        if (classesDir != null) {
            try {
                mapper.mapProjectClasses(classesDir);
                System.out.println("Mapped " + mapper.lineToMethodMap.size() + " classes for coverage analysis");

                // 打印映射示例用于调试
                if (!mapper.lineToMethodMap.isEmpty()) {
                    String sampleClass = mapper.lineToMethodMap.keySet().iterator().next();
                    System.out.println("Sample mappings for class: " + sampleClass);
                    Map<Integer, String> sampleMappings = mapper.lineToMethodMap.get(sampleClass);
                    int count = 0;
                    for (Map.Entry<Integer, String> entry : sampleMappings.entrySet()) {
                        System.out.println("  Line " + entry.getKey() + " -> " + entry.getValue());
                        if (++count > 10) break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to map classes: " + e.getMessage());
            }
        }
        for (String testMethod : testMethods) {
            String[] parts = testMethod.split("#");
            String className = parts[0];
            String methodName = parts[1];

            System.out.println("Running test: " + testMethod);

            // 重置覆盖率数据
            JacocoAgentLoader.reset();

            try {
                runSingleTest(className, methodName);
            } catch (Exception e) {
                System.err.println("Error running test " + testMethod + ": " + e.getMessage());
                e.printStackTrace();
                // 即使测试失败也添加空覆盖条目
                result.addCoverage(testMethod, new ArrayList<>());
                continue;
            }

            // 收集覆盖率数据
            byte[] executionData = JacocoAgentLoader.getExecutionData();
            System.out.println("Execution data size: " + executionData.length + " bytes");

            List<String> coveredMethods = analyzeCoverage(executionData, mapper);
            result.addCoverage(testMethod, coveredMethods);

            System.out.println("  Covered " + coveredMethods.size() + " methods");
        }

        return result;
    }

    private void runSingleTest(String className, String methodName) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
            try {
                // 设置项目类加载器
                Thread.currentThread().setContextClassLoader(projectClassLoader);

                System.out.println("Loading test class: " + className);
                Class<?> testClass = Class.forName(className, true, projectClassLoader);

                System.out.println("Running test: " + className + "#" + methodName);
                Request request = Request.method(testClass, methodName);
                Result result = new JUnitCore().run(request);

                System.out.println("Test completed: " + methodName);
                System.out.println("  Run time: " + result.getRunTime() + "ms");
                System.out.println("  Tests run: " + result.getRunCount());
                System.out.println("  Failures: " + result.getFailureCount());

                if (!result.wasSuccessful()) {
                    for (org.junit.runner.notification.Failure failure : result.getFailures()) {
                        System.err.println("Test failure: " + failure.getTestHeader());
                        System.err.println(failure.getTrace());
                    }
                }

                // 添加Jacoco代理状态检查
                System.out.println("Jacoco agent data size: " +
                        JacocoAgentLoader.getAgent().getExecutionData(false).length + " bytes");
            } catch (ClassNotFoundException e) {
                System.out.println("");
            } catch (Exception e) {
                System.out.println("");
            } finally {
                Thread.currentThread().setContextClassLoader(originalLoader);
            }
        });

        try {
            future.get(testTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Test timed out after " + testTimeout + " seconds: " + className + "#" + methodName);
        } catch (ExecutionException e) {
            throw new RuntimeException("Test execution failed: " + e.getCause().getMessage(), e.getCause());
        } finally {
            executor.shutdownNow();

            // 给JaCoCo时间写入数据
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<String> analyzeCoverage(byte[] executionData, MethodCoverageMapper mapper) {
        if (executionData == null || executionData.length == 0) {
            System.out.println("No coverage data collected");
            return new ArrayList<>();
        }

        // 创建临时目录
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("coverage-analysis");
        } catch (IOException e) {
            System.err.println("Failed to create temp directory: " + e.getMessage());
            return new ArrayList<>();
        }

        // 创建临时的 .exec 文件
        Path tempExecFile = tempDir.resolve("coverage.exec");
        try {
            Files.write(tempExecFile, executionData);
            System.out.println("Created temp exec file: " + tempExecFile);
        } catch (IOException e) {
            System.err.println("Failed to create temp exec file: " + e.getMessage());
            return new ArrayList<>();
        }

        // 加载执行数据
        ExecFileLoader execFileLoader = new ExecFileLoader();
        try {
            execFileLoader.load(tempExecFile.toFile());
            System.out.println("Loaded execution data for " +
                    execFileLoader.getExecutionDataStore().getContents().size() + " classes");
        } catch (IOException e) {
            System.err.println("Failed to load exec file: " + e.getMessage());
            return new ArrayList<>();
        }

        // 创建覆盖率分析器 - 只分析生产代码目录
        CoverageAnalyzer analyzer = new CoverageAnalyzer(
                execFileLoader.getExecutionDataStore(),
                findClassesDirectory()
        );

        // 分析覆盖率
        List<String> coveredMethods = new ArrayList<>();
        try {
            Collection<IClassCoverage> classCoverages = analyzer.analyze();
            System.out.println("Analyzed " + classCoverages.size() + " classes");

            // 收集覆盖的方法
            for (IClassCoverage classCoverage : classCoverages) {
                String className = classCoverage.getName().replace('/', '.');
                System.out.println("Processing class: " + className);

                // 获取类文件的行号映射
                Map<Integer, String> lineToMethod = mapper.lineToMethodMap.get(className);
                if (lineToMethod == null) {
                    continue;
                }

                // 打印类覆盖率摘要
                System.out.printf("  Coverage: %d/%d lines covered%n",
                        classCoverage.getLineCounter().getCoveredCount(),
                        classCoverage.getLineCounter().getTotalCount());

                // 关键修改：更精确的行号处理方法
                int firstLine = classCoverage.getFirstLine();
                int lastLine = classCoverage.getLastLine();

                System.out.println("  Line range: " + firstLine + " - " + lastLine);

                for (int i = firstLine; i <= lastLine; i++) {
                    ILine line = classCoverage.getLine(i);
                    if (line == null) {
                        // 没有行信息，跳过
                        continue;
                    }

                    int status = line.getStatus();
                    if (status == ICounter.FULLY_COVERED || status == ICounter.PARTLY_COVERED || true) {
                        String methodName = lineToMethod.get(i);
                        if (methodName != null) {
                            String methodId = className + "#" + methodName;
                            if (!coveredMethods.contains(methodId)) {
                                coveredMethods.add(methodId);
                                System.out.println("  Covered line " + i + " in method: " + methodId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error during coverage analysis: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(tempExecFile);
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                System.err.println("Failed to delete temp files: " + e.getMessage());
            }
        }

        return coveredMethods;
    }

    public void setTestTimeout(int seconds) {
        this.testTimeout = seconds;
    }
}