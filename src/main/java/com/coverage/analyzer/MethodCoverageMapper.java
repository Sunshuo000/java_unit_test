package com.coverage.analyzer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MethodCoverageMapper {
    public final Map<String, Map<Integer, String>> lineToMethodMap = new ConcurrentHashMap<>();
    private final Map<String, List<MethodRange>> methodRanges = new ConcurrentHashMap<>();

    public void mapProjectClasses(Path projectPath) throws IOException {
        System.out.println("Mapping classes in: " + projectPath);
        long start = System.currentTimeMillis();
        int classCount = 0;
        int lineCount = 0;

        for (Path classFile : (Iterable<Path>) Files.walk(projectPath)
                .filter(path -> path.toString().endsWith(".class"))
                .parallel()::iterator) {
            mapClassFile(classFile);
            classCount++;
            lineCount += lineToMethodMap.getOrDefault(classFile.getFileName().toString(), new HashMap<>()).size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Mapped " + classCount + " classes (" + lineCount + " lines) in " + (end - start) + "ms");
    }

    private void mapClassFile(Path classFile) {
        try (InputStream is = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(is);

            // 使用 ASM Tree API 获取行号信息
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_DEBUG);

            String className = classNode.name.replace('/', '.');
            Map<Integer, String> lineMap = new HashMap<>();
            List<MethodRange> ranges = new ArrayList<>();

            for (MethodNode method : (List<MethodNode>) classNode.methods) {
                // 跳过编译器生成的方法
                if (method.name.startsWith("lambda$") || method.name.contains("$")) {
                    continue;
                }

                // 跳过构造函数和静态初始化块
                if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) {
                    continue;
                }

                // 记录方法的行号
                if (method.instructions != null) {
                    method.instructions.forEach(insn -> {
                        if (insn instanceof LineNumberNode) {
                            LineNumberNode lineNode = (LineNumberNode) insn;
                            lineMap.put(lineNode.line, method.name);
                        }
                    });
                }
            }

            lineToMethodMap.put(className, lineMap);
            System.out.println("Mapped " + lineMap.size() + " lines for class: " + className);
        } catch (IOException e) {
            System.err.println("Error mapping class: " + classFile);
            e.printStackTrace();
        }
    }

    public String getMethodForLine(String className, int lineNumber) {
        // 1. 尝试直接行号映射
        Map<Integer, String> classMap = lineToMethodMap.get(className);
        if (classMap != null) {
            String method = classMap.get(lineNumber);
            if (method != null) {
                return method;
            }
        }

        // 2. 尝试方法范围匹配
        List<MethodRange> ranges = methodRanges.get(className);
        if (ranges != null) {
            for (MethodRange range : ranges) {
                if (lineNumber >= range.startLine && lineNumber <= range.endLine) {
                    return range.methodName;
                }
            }
        }

        // 3. 尝试最近的映射
        if (classMap != null) {
            int closestLine = -1;
            String closestMethod = null;
            for (Map.Entry<Integer, String> entry : classMap.entrySet()) {
                int line = entry.getKey();
                if (line <= lineNumber && line > closestLine) {
                    closestLine = line;
                    closestMethod = entry.getValue();
                }
            }
            return closestMethod;
        }

        return null;
    }

    private static class MethodRange {
        final String methodName;
        final int startLine;
        final int endLine;

        MethodRange(String methodName, int startLine, int endLine) {
            this.methodName = methodName;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }
}