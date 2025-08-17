package com.coverage.analyzer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MethodCoverageMapper {
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

        long end = System.currentTimeMillis();
        System.out.println("Mapped " + classCount + " classes in " + (end - start) + "ms");
    }

    private void mapClassFile(Path classFile) {
        try (InputStream is = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(is);
            ClassLineMethodMapper mapper = new ClassLineMethodMapper();
            // 关键修改：移除 SKIP_DEBUG 标志以获取行号信息
            reader.accept(mapper, ClassReader.SKIP_FRAMES);

            if (!mapper.lineToMethod.isEmpty()) {
                lineToMethodMap.put(mapper.className, mapper.lineToMethod);
                System.out.println("Mapped " + mapper.lineToMethod.size() +
                        " lines for class: " + mapper.className);
            }
        } catch (IOException e) {
            System.err.println("Error mapping class: " + classFile);
            e.printStackTrace();
        }
    }

    private static class ClassLineMethodMapper extends ClassVisitor {
        String className;
        Map<Integer, String> lineToMethod = new HashMap<>();
        String currentMethod;

        public ClassLineMethodMapper() {
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
            return new MethodLineMapper(this);
        }
    }

    private static class MethodLineMapper extends MethodVisitor {
        private final ClassLineMethodMapper classMapper;

        public MethodLineMapper(ClassLineMethodMapper classMapper) {
            super(Opcodes.ASM9);
            this.classMapper = classMapper;
        }

        @Override
        public void visitLineNumber(int line, org.objectweb.asm.Label start) {
            // 关键修改：确保行号被正确处理
            classMapper.lineToMethod.put(line, classMapper.currentMethod);
            super.visitLineNumber(line, start);
        }
    }

    public void printMappingsForClass(String className) {
        Map<Integer, String> mappings = lineToMethodMap.get(className);
        if (mappings != null) {
            System.out.println("Line mappings for " + className + ":");
            mappings.forEach((line, method) ->
                    System.out.println("  Line " + line + " -> " + method));
        } else {
            System.out.println("No mappings found for class: " + className);
        }
    }
}