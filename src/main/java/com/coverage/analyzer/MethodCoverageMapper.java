package com.coverage.analyzer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MethodCoverageMapper {
    public final Map<String, Map<String, int[]>> methodRanges = new ConcurrentHashMap<>();
    public final Map<String, Map<Integer, String>> lineToMethodMap = new ConcurrentHashMap<>();

    public void mapProjectClasses(Path projectPath) throws IOException {
        System.out.println("Mapping classes in: " + projectPath);
        long start = System.currentTimeMillis();
        int classCount = 0;

        for (Path classFile : (Iterable<Path>) Files.walk(projectPath)
                .filter(path -> path.toString().endsWith(".class"))
                ::iterator) {
            mapClassFile(classFile);
            classCount++;
        }

        // 基于方法范围创建行号映射
        for (Map.Entry<String, Map<String, int[]>> entry : methodRanges.entrySet()) {
            String className = entry.getKey();
            Map<String, int[]> methods = entry.getValue();
            Map<Integer, String> lineMap = new HashMap<>();

            for (Map.Entry<String, int[]> method : methods.entrySet()) {
                String methodName = method.getKey();
                int[] range = method.getValue();

                for (int line = range[0]; line <= range[1]; line++) {
                    lineMap.put(line, methodName);
                }
            }

            lineToMethodMap.put(className, lineMap);
            System.out.println("Created line mapping for " + className +
                    " with " + lineMap.size() + " entries");
        }

        long end = System.currentTimeMillis();
        System.out.println("Mapped " + classCount + " classes in " + (end - start) + "ms");
    }

    private void mapClassFile(Path classFile) {
        try (InputStream is = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(is);
            ClassMethodRangeMapper mapper = new ClassMethodRangeMapper();
            reader.accept(mapper, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

            if (!mapper.methodRanges.isEmpty()) {
                methodRanges.put(mapper.className, mapper.methodRanges);
                System.out.println("Mapped " + mapper.methodRanges.size() +
                        " methods for class: " + mapper.className);
            }
        } catch (IOException e) {
            System.err.println("Error mapping class: " + classFile);
            e.printStackTrace();
        }
    }

    private static class ClassMethodRangeMapper extends ClassVisitor {
        String className;
        Map<String, int[]> methodRanges = new HashMap<>();
        String currentMethod;
        int minLine = Integer.MAX_VALUE;
        int maxLine = Integer.MIN_VALUE;

        public ClassMethodRangeMapper() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.className = name.replace('/', '.');
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            // 跳过特殊方法
            if ("<init>".equals(name) || "<clinit>".equals(name) ||
                    name.startsWith("lambda$") || name.contains("$")) {
                return null;
            }

            currentMethod = name;
            minLine = Integer.MAX_VALUE;
            maxLine = Integer.MIN_VALUE;

            return new MethodRangeMapper(this);
        }

        @Override
        public void visitEnd() {
            // 保存当前方法范围（如果有）
            if (minLine != Integer.MAX_VALUE && maxLine != Integer.MIN_VALUE) {
                methodRanges.put(currentMethod, new int[]{minLine, maxLine});
            }
            super.visitEnd();
        }
    }

    private static class MethodRangeMapper extends MethodVisitor {
        private final ClassMethodRangeMapper classMapper;

        public MethodRangeMapper(ClassMethodRangeMapper classMapper) {
            super(Opcodes.ASM9);
            this.classMapper = classMapper;
        }

        @Override
        public void visitLineNumber(int line, org.objectweb.asm.Label start) {
            updateLineRange(line);
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String descriptor, boolean isInterface) {
            // 方法调用可能会改变行号范围
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitEnd() {
            // 保存方法范围
            if (classMapper.minLine != Integer.MAX_VALUE &&
                    classMapper.maxLine != Integer.MIN_VALUE) {
                classMapper.methodRanges.put(
                        classMapper.currentMethod,
                        new int[]{classMapper.minLine, classMapper.maxLine}
                );
            }
            super.visitEnd();
        }

        private void updateLineRange(int line) {
            if (line < classMapper.minLine) classMapper.minLine = line;
            if (line > classMapper.maxLine) classMapper.maxLine = line;
        }
    }

    public String getMethodForLine(String className, int lineNumber) {
        Map<Integer, String> lineMap = lineToMethodMap.get(className);
        if (lineMap != null) {
            return lineMap.get(lineNumber);
        }

        // 回退到范围匹配
        Map<String, int[]> ranges = methodRanges.get(className);
        if (ranges != null) {
            for (Map.Entry<String, int[]> entry : ranges.entrySet()) {
                int[] range = entry.getValue();
                if (lineNumber >= range[0] && lineNumber <= range[1]) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    public void printMappingsForClass(String className) {
        Map<String, int[]> ranges = methodRanges.get(className);
        if (ranges != null) {
            System.out.println("Method ranges for " + className + ":");
            ranges.forEach((method, range) ->
                    System.out.println("  " + method + ": [" + range[0] + "-" + range[1] + "]"));
        } else {
            System.out.println("No mappings found for class: " + className);
        }
    }
}