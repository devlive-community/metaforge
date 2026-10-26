package org.devlive.metaforge.plugin.service;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Service;
import org.devlive.metaforge.plugin.annotation.InjectService;
import org.devlive.metaforge.plugin.scanner.ServiceAnnotationScanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;

@Slf4j
public class ServiceSpiLoader
{
    private ServiceSpiLoader() {}

    /**
     * 加载服务实现,同时支持SPI和注解方式
     * Load service implementations, supporting both SPI and annotation methods
     *
     * @param serviceType 服务类型（必须继承自Service）
     * service type (must extend Service)
     * @param basePackage 扫描注解的基础包路径
     * base package path for annotation scanning
     * @param classLoader 类加载器
     * class loader
     * @return 服务绑定
     * service bindings
     */
    public static ServiceBindings loadServices(Class<? extends Service> serviceType, String basePackage, ClassLoader classLoader)
    {
        ServiceBindings bindings = loadServices(serviceType, classLoader);

        // 扫描注解
        // Scan annotations
        Set<Class<?>> annotatedServices = ServiceAnnotationScanner.scanServices(basePackage, classLoader);
        log.info("Found {} annotated services", annotatedServices.size());
        log.info("Scanned annotated services from package: {}", basePackage);
        log.debug("Annotated services: {}", annotatedServices);
        for (Class<?> serviceImpl : annotatedServices) {
            InjectService annotation = serviceImpl.getAnnotation(InjectService.class);
            if (annotation != null) {
                Class<?>[] serviceInterfaces = annotation.value();
                if (serviceInterfaces.length == 0) {
                    // 如果没有指定接口,使用类实现的所有接口
                    // If no interface is specified, use all interfaces implemented by the class
                    @SuppressWarnings("unchecked")
                    Class<? extends Service> impl = (Class<? extends Service>) serviceImpl;
                    addServiceBindings(bindings, impl, serviceType);
                }
                else {
                    // 添加指定的接口绑定
                    // Add specified interface bindings
                    for (Class<?> iface : serviceInterfaces) {
                        if (Service.class.isAssignableFrom(iface) && serviceType.isAssignableFrom(iface)) {
                            @SuppressWarnings("unchecked")
                            Class<? extends Service> service = (Class<? extends Service>) iface;
                            @SuppressWarnings("unchecked")
                            Class<? extends Service> impl = (Class<? extends Service>) serviceImpl;
                            bindings.addBinding(service, impl);
                            log.debug("Added annotated binding: {} -> {}", iface.getName(), serviceImpl.getName());
                        }
                    }
                }
            }
        }

        return bindings;
    }

    private static void addServiceBindings(ServiceBindings bindings, Class<? extends Service> serviceImpl, Class<? extends Service> serviceType)
    {
        // 添加直接绑定
        // Add direct binding
        if (serviceType.isAssignableFrom(serviceImpl)) {
            bindings.addBinding(serviceType, serviceImpl);
            log.debug("Added direct binding: {} -> {}", serviceType.getName(), serviceImpl.getName());
        }

        // 检查并添加接口绑定
        // Check and add interface bindings
        for (Class<?> iface : getAllInterfaces(serviceImpl)) {
            if (serviceType.isAssignableFrom(iface)) {
                @SuppressWarnings("unchecked")
                Class<? extends Service> serviceInterface = (Class<? extends Service>) iface;
                bindings.addBinding(serviceInterface, serviceImpl);
                log.debug("Added interface binding: {} -> {}", serviceInterface.getName(), serviceImpl.getName());
            }
        }
    }

    /**
     * 加载服务实现
     * Load service implementations
     *
     * @param serviceType 服务类型（必须继承自Service）
     * service type (must extend Service)
     * @return 服务绑定
     * service bindings
     * @throws IllegalArgumentException 如果服务类型不是Service的子类
     * if service type is not a Service subclass
     */
    public static ServiceBindings loadServices(Class<? extends Service> serviceType)
    {
        return loadServices(serviceType, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 加载服务实现
     * Load service implementations
     *
     * @param serviceType 服务类型（必须继承自Service）
     * service type (must extend Service)
     * @param classLoader 类加载器
     * class loader
     * @return 服务绑定
     * service bindings
     * @throws IllegalArgumentException 如果服务类型不是Service的子类
     * if service type is not a Service subclass
     */
    public static ServiceBindings loadServices(Class<? extends Service> serviceType, ClassLoader classLoader)
    {
        ServiceBindings bindings = new ServiceBindings();

        log.debug("Loading services for type {} using ClassLoader: {}", serviceType.getName(), classLoader.getClass().getName());

        try {
            // 使用指定的类加载器检查服务定义文件
            // Check for service definitions using the specified class loader
            String servicePath = "META-INF/services/" + serviceType.getName();
            Enumeration<URL> resources = classLoader.getResources(servicePath);

            // 添加一个 Set 来跟踪已处理的实现类
            // Add a Set to track processed implementation classes
            Set<String> processedImplementations = Sets.newHashSet();

            boolean found = false;
            while (resources.hasMoreElements()) {
                found = true;
                URL url = resources.nextElement();
                log.debug("Found service file: {}", url);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), Charset.defaultCharset()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty() && !line.startsWith("#")) {
                            line = line.trim();

                            // 如果这个实现类已经处理过，跳过
                            // Skip if this implementation class has already been processed
                            if (processedImplementations.contains(line)) {
                                log.debug("Skipping already processed implementation class: {}", line);
                                continue;
                            }
                            processedImplementations.add(line);

                            log.debug("Service implementation defined in file: {}", line);
                            try {
                                // 使用指定的类加载器加载实现类
                                // Load the implementation class using the specified class loader
                                Class<?> implementationClass = classLoader.loadClass(line);
                                log.debug("Successfully loaded implementation class: {}", implementationClass.getName());

                                if (Service.class.isAssignableFrom(implementationClass)) {
                                    @SuppressWarnings("unchecked")
                                    Class<? extends Service> serviceImpl = (Class<? extends Service>) implementationClass;

                                    // 添加直接绑定
                                    // Add direct binding
                                    if (serviceType.isAssignableFrom(serviceImpl)) {
                                        // 导致无法进行多个实现类扫描
                                        // Not causing multiple implementation scanning
//                                        if (!bindings.getBindings().containsKey(serviceType)) {
//                                            bindings.addBinding(serviceType, serviceImpl);
//                                            log.debug("Added direct binding: {} -> {}", serviceType.getName(), serviceImpl.getName());
//                                        }
                                        bindings.addBinding(serviceType, serviceImpl);
                                        log.debug("Added direct binding: {} -> {}", serviceType.getName(), serviceImpl.getName());
                                    }

                                    // 检查并添加接口绑定
                                    // Check and add interface binding
                                    for (Class<?> iface : getAllInterfaces(serviceImpl)) {
                                        if (serviceType.isAssignableFrom(iface)) {
                                            @SuppressWarnings("unchecked")
                                            Class<? extends Service> serviceInterface = (Class<? extends Service>) iface;
                                            // 导致无法进行多个实现类扫描
                                            // Not causing multiple implementation scanning
//                                            if (!bindings.getBindings().containsKey(serviceInterface)) {
//                                                bindings.addBinding(serviceInterface, serviceImpl);
//                                                log.debug("Added interface binding: {} -> {}", serviceInterface.getName(), serviceImpl.getName());
//                                            }
                                            bindings.addBinding(serviceInterface, serviceImpl);
                                            log.debug("Added interface binding: {} -> {}", serviceInterface.getName(), serviceImpl.getName());
                                        }
                                    }
                                }
                            }
                            catch (ClassNotFoundException e) {
                                log.error("Failed to load implementation class: {}", line, e);
                            }
                        }
                    }
                }
            }

            if (!found) {
                log.warn("No service definition files found for {}", serviceType.getName());
            }
        }
        catch (IOException e) {
            log.error("Error loading services for type: {}", serviceType.getName(), e);
        }

        // 记录找到的所有绑定
        // Record all found bindings
        bindings.getBindings().forEach((service, impl) ->
                log.debug("Service: {} has implementations: {}", service.getName(), impl.getName())
        );

        return bindings;
    }

    private static Set<Class<?>> getAllInterfaces(Class<?> clazz)
    {
        Set<Class<?>> interfaces = Sets.newHashSet();
        while (clazz != null) {
            interfaces.addAll(Arrays.asList(clazz.getInterfaces()));
            clazz = clazz.getSuperclass();
        }
        return interfaces;
    }
}
