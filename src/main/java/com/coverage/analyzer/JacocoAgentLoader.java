package com.coverage.analyzer;

import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;
import org.jacoco.agent.rt.internal_4742761.core.runtime.AgentOptions;
import org.jacoco.agent.rt.internal_4742761.Agent;

import java.io.IOException;

public class JacocoAgentLoader {
    private static IAgent agent = null;

    public static synchronized void loadAgent() {
        if (agent == null) {
            try {
                // 1. 创建代理配置
                AgentOptions options = new AgentOptions();
                options.setDestfile("jacoco.exec"); // 设置输出文件
                options.setOutput(AgentOptions.OutputMode.none); // 禁用文件输出
                options.setAppend(false);

                // 2. 使用正确的初始化方式
                agent = Agent.getInstance(options);
                System.out.println("Jacoco agent initialized. Version: " + agent.getVersion());
            } catch (Exception e) {
                System.err.println("Failed to load Jacoco agent: " + e.getMessage());
                e.printStackTrace();

                // 3. 备选方案：尝试通过RT获取
                try {
                    agent = RT.getAgent();
                    System.out.println("Jacoco agent loaded via RT");
                } catch (IllegalStateException rtEx) {
                    throw new RuntimeException("ERROR: Failed to load Jacoco agent via both methods. Please ensure the agent is properly initialized.", rtEx);
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