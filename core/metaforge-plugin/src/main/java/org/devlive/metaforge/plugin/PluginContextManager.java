package org.devlive.metaforge.plugin;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.loader.PluginClassLoader;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

/**
 * 插件上下文管理器
 * Plugin Context Manager
 */
@Slf4j
public class PluginContextManager
{
    private PluginContextManager() {}

    // 使用WeakHashMap避免内存泄漏
    // Use WeakHashMap to avoid memory leaks
    private static final Map<Thread, PluginClassLoader> contextClassLoaders = new WeakHashMap<>();

    /**
     * 设置当前线程的插件类加载器
     * Set the plugin class loader for the current thread
     *
     * @param classLoader 插件类加载器
     * Plugin class loader
     */
    public static void setPluginClassLoader(PluginClassLoader classLoader)
    {
        log.debug("Setting plugin class loader: {} for plugin: {}", classLoader, classLoader.getPluginName());
        synchronized (contextClassLoaders) {
            contextClassLoaders.put(Thread.currentThread(), classLoader);
        }
    }

    /**
     * 获取当前线程的插件类加载器
     * Get the plugin class loader for the current thread
     *
     * @return 插件类加载器
     * Plugin class loader
     */
    public static ClassLoader getPluginClassLoader()
    {
        synchronized (contextClassLoaders) {
            PluginClassLoader loader = contextClassLoaders.get(Thread.currentThread());
            if (loader != null) {
                log.debug("Returning plugin class loader: {} for plugin: {}",
                        loader, loader.getPluginName());
                return loader;
            }
        }
        log.debug("Returning thread's context class loader: {}",
                Thread.currentThread().getContextClassLoader());
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 清除当前线程的插件类加载器
     * Clear the plugin class loader for the current thread
     */
    public static void clearPluginClassLoader()
    {
        synchronized (contextClassLoaders) {
            PluginClassLoader removed = contextClassLoaders.remove(Thread.currentThread());
            if (removed != null) {
                log.debug("Cleared plugin class loader for plugin: {}", removed.getPluginName());
            }
        }
    }

    /**
     * 在指定的类加载器上下文中执行任务
     * Execute a task within the specified class loader context
     *
     * @param classLoader 类加载器 Class loader
     * @param task 任务 Task
     * @param <T> 返回值类型 Return type
     * @return 任务的执行结果 Result of the task execution
     * @throws Exception 任务执行过程中发生的异常 Exception occurred during task execution
     */
    public static <T> T runWithClassLoader(PluginClassLoader classLoader, Callable<T> task)
            throws Exception
    {
        log.debug("Running task with class loader: {}", classLoader);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            setPluginClassLoader(classLoader);
            Thread.currentThread().setContextClassLoader(classLoader);
            return task.call();
        }
        finally {
            clearPluginClassLoader();
            Thread.currentThread().setContextClassLoader(previous);
            log.debug("Restored previous class loader: {}", previous);
        }
    }
}
