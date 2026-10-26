package org.devlive.metaforge.plugin.scanner;

import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Service;
import org.devlive.metaforge.plugin.annotation.InjectService;

import java.io.IOException;
import java.util.Set;

@Slf4j
public class ServiceAnnotationScanner
{
    private ServiceAnnotationScanner() {}

    /**
     * 扫描指定包下带有 @InjectService 注解的类
     * Scan classes with @InjectService annotation in the specified package
     *
     * @param basePackage 基础包路径
     * base package path
     * @param classLoader 类加载器
     * class loader
     * @return 扫描到的服务类集合
     * scanned service class collection
     */
    public static Set<Class<?>> scanServices(String basePackage, ClassLoader classLoader)
    {
        Set<Class<?>> services = Sets.newHashSet();
        try {
            ClassPath classPath = ClassPath.from(classLoader);
            for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClassesRecursive(basePackage)) {
                try {
                    Class<?> clazz = classInfo.load();
                    if (clazz.isAnnotationPresent(InjectService.class) && Service.class.isAssignableFrom(clazz)) {
                        services.add(clazz);
                        log.debug("Found service implementation class: {}", clazz.getName());
                    }
                }
                catch (Throwable e) {
                    log.warn("Failed to load class: {}", classInfo.getName(), e);
                }
            }
        }
        catch (IOException e) {
            log.error("Error scanning for services in package: {}", basePackage, e);
        }
        return services;
    }
}
