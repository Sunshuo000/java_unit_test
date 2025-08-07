package com.coverage.analyzer;

import org.jacoco.agent.rt.internal_3570298.core.data.ExecutionData;
import org.jacoco.agent.rt.internal_3570298.core.data.ExecutionDataReader;
import org.jacoco.agent.rt.internal_3570298.core.data.ExecutionDataStore;
import org.jacoco.agent.rt.internal_3570298.core.data.IExecutionDataVisitor;
import org.jacoco.agent.rt.internal_3570298.core.data.ISessionInfoVisitor;
import org.jacoco.agent.rt.internal_3570298.core.data.SessionInfo;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import com.coverage.analyzer.models.CoverageResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CoverageRunner {
    private final Path projectPath;
    private final Path rulesetPath;
    private final MethodCoverageMapper mapper;
    private final Set<String> coveredClasses = new HashSet<>();
    private int testTimeout = 30; // 默认超时30秒

    public CoverageRunner(Path projectPath, Path rulesetPath, MethodCoverageMapper mapper) {
        this.projectPath = projectPath;
        this.rulesetPath = rulesetPath;
        this.mapper = mapper;
    }
}