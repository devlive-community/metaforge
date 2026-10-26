package org.devlive.metaforge.common.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnvironmentUtils
{
    private EnvironmentUtils()
    {}

    private static final String CLASSPATH = System.getProperty("java.class.path", "");
    private static final String JAVA_AGENT = System.getProperty("java.agent", "");
    private static final String COMMAND_LINE = System.getProperty("sun.java.command", "");
    private static final String VM_ARGS = System.getProperty("java.vm.name", "");

    /**
     * 判断是否在 IDE 环境中运行
     * Check if in IDE environment
     */
    public static boolean isIdeEnvironment()
    {
        return isIntelliJ() ||
                isEclipse() ||
                isNetBeans() ||
                isVsCode();
    }

    /**
     * 判断是否在 IntelliJ IDEA 中运行
     * Check if in IntelliJ IDEA
     */
    public static boolean isIntelliJ()
    {
        return CLASSPATH.contains("idea_rt.jar") ||
                JAVA_AGENT.contains("jetbrains") ||
                COMMAND_LINE.contains("com.intellij");
    }

    /**
     * 判断是否在 Eclipse 中运行
     * Check if in Eclipse
     */
    public static boolean isEclipse()
    {
        return CLASSPATH.contains("eclipse") ||
                COMMAND_LINE.contains("eclipse") ||
                System.getProperty("eclipse.launcher") != null ||
                System.getProperty("eclipse.application") != null;
    }

    /**
     * 判断是否在 NetBeans 中运行
     * Check if in NetBeans
     */
    public static boolean isNetBeans()
    {
        return CLASSPATH.contains("netbeans") ||
                COMMAND_LINE.contains("org.netbeans");
    }

    /**
     * 判断是否在 VS Code 中运行
     * Check if in VS Code
     */
    public static boolean isVsCode()
    {
        return CLASSPATH.contains("vscode") ||
                COMMAND_LINE.contains("vscode") ||
                System.getenv("VSCODE_CLI") != null;
    }

    /**
     * 获取当前 IDE 名称
     * Get current IDE name
     */
    public static String getCurrentIde()
    {
        if (isIntelliJ()) {
            return "IntelliJ IDEA";
        }
        if (isEclipse()) {
            return "Eclipse";
        }
        if (isNetBeans()) {
            return "NetBeans";
        }
        if (isVsCode()) {
            return "VS Code";
        }
        return "Unknown";
    }

    /**
     * 打印当前环境信息
     * Print current environment information
     */
    public static void printEnvironmentInfo()
    {
        log.info("Environment Information:");
        if (isIdeEnvironment()) {
            log.info("ClassPath [ {} ]", CLASSPATH);
        }
        log.info("IDE [ {} ]", getCurrentIde());
        log.info("Is IDE Environment [ {} ]", isIdeEnvironment());
        log.info("Java Agent [ {} ]", JAVA_AGENT);
        log.info("Command Line [ {} ]", COMMAND_LINE);
        log.info("VM Args [ {} ]", VM_ARGS);
    }
}
