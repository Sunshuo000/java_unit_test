package com.coverage.analyzer;

import com.coverage.analyzer.models.ProjectStats;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectParser {
    private final Path projectPath;
    private final ProjectStats stats = new ProjectStats();
    private final List<String> testMethods = new ArrayList<>();

    public ProjectParser(Path projectPath) {
        this.projectPath = projectPath;
        stats.setLocation(projectPath.toString());
    }

    public ProjectStats parse() throws IOException {
        Files.walk(projectPath)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(this::processJavaFile);

        stats.setNumTestMethods(testMethods.size());
        return stats;
    }

    private void processJavaFile(Path javaFile) {
        stats.incrementJavaFiles();

        try {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(javaFile);
            if (!parseResult.isSuccessful()) {
                System.err.println("Parse errors in file: " + javaFile);
                return;
            }

            parseResult.getResult().ifPresent(cu -> {
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                    // 跳过接口
                    if (classDecl.isInterface()) {
                        return;
                    }

                    stats.incrementClasses();
                    processClass(classDecl, cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString())
                            .orElse(""));
                });
            });
        } catch (IOException e) {
            System.err.println("Error parsing file: " + javaFile);
        }
    }

    private void processClass(ClassOrInterfaceDeclaration classDecl, String packageName) {
        String className = packageName.isEmpty() ?
                classDecl.getNameAsString() :
                packageName + "." + classDecl.getNameAsString();

        classDecl.getMethods().forEach(method -> {
            stats.incrementMethods();
            if (isTestMethod(method)) {
                testMethods.add(className + "#" + method.getNameAsString());
            }
        });
    }

    private boolean isTestMethod(MethodDeclaration method) {
        // 扩展测试检测规则
        return method.getAnnotationByName("Test").isPresent() ||
                method.getNameAsString().startsWith("test") ||
                method.getAnnotationByName("ParameterizedTest").isPresent() ||
                method.getAnnotationByName("RepeatedTest").isPresent() ||
                method.getAnnotationByName("TestFactory").isPresent();
    }

    public List<String> getTestMethods() {
        return testMethods;
    }
}