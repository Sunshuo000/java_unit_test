package com.coverage.analyzer;

import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;
import org.jacoco.agent.rt.internal_3570298.Agent;
import org.jacoco.agent.rt.internal_3570298.core.runtime.AgentOptions;

import java.io.IOException;
import java.lang.management.ManagementFactory;

public class JacocoAgentLoader {
    private static IAgent agent = null;

    public static synchronized void loadAgent() {
        if (agent == null) {
            try {
                System.out.println("Attempting to load Jacoco agent via standard method");
                agent = RT.getAgent();
                System.out.println("Jacoco agent loaded via RT. Version: " + agent.getVersion());
            } catch (IllegalStateException rtEx) {
                System.out.println("Standard loading failed, trying alternative method");

                try {
                    // 2. 备选加载方式
                    String vmName = ManagementFactory.getRuntimeMXBean().getName();
                    String pid = vmName.split("@")[0];

                    System.out.println("Attempting to load Jacoco agent dynamically for PID: " + pid);

                    AgentOptions options = new AgentOptions();
                    options.setOutput("none");
                    options.setAppend(false);

                    agent = Agent.getInstance(options);
                    System.out.println("Jacoco agent dynamically loaded. Version: " + agent.getVersion());
                } catch (Exception e) {
                    System.err.println("Failed to load Jacoco agent: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Jacoco agent initialization failed", e);
                }
            }
        }
    }

    public static void reset() {
        if (agent != null) {
            agent.reset();
        }
    }

    public static byte[] getExecutionData() {
        if (agent != null) {
            try {
                // 确保数据写入完成
                agent.dump(false);
                return agent.getExecutionData(false);
            } catch (IOException e) {
                System.err.println("Failed to dump coverage data: " + e.getMessage());
            }
        }
        return new byte[0];
    }

    public static IAgent getAgent() {
        return agent;
    }
}