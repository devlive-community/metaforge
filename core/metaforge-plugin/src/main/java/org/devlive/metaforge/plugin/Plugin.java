package org.devlive.metaforge.plugin;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.devlive.metaforge.plugin.loader.PluginClassLoader;
import org.devlive.metaforge.plugin.service.ServiceBindings;
import org.devlive.metaforge.plugin.service.ServiceNotFoundException;
import org.devlive.metaforge.plugin.service.ServiceSpiLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public abstract class Plugin
        extends AbstractModule
{
    private final Map<Class<? extends Service>, Boolean> binds = new HashMap<>();

    private volatile String cachedVersion;

    // 依赖注入器
    // Dependency injector
    @Setter
    @Getter
    private Injector injector;

    @Getter
    @Setter
    private String classLoader;

    // 插件专用类加载器
    // Plugin-specific class loader
    @Getter
    private PluginClassLoader pluginClassLoader;

    // 插件唯一标记
    // Plugin unique key
    @Getter
    @Setter
    private String key;

    /**
     * 设置插件类加载器
     * Set plugin class loader
     *
     * @param classLoader 插件类加载器
     * Plugin class loader
     */
    public void setPluginClassLoader(PluginClassLoader classLoader)
    {
        this.pluginClassLoader = classLoader;
        this.classLoader = classLoader.getClass().getName();
    }

    /**
     * 类型安全的服务绑定方法
     * Type-safe service binding method
     *
     * @param service 服务接口类型
     * service interface type
     * @param implementation 服务实现类型
     * service implementation type
     */
    @SuppressWarnings("unchecked")
    private <T extends Service> void bindService(Class<? extends Service> service, Class<? extends Service> implementation, boolean multiple, String qualifier)
    {
        // 确保实现类是服务接口的子类
        // Ensure implementation class is a subclass of service interface
        if (!service.isAssignableFrom(implementation)) {
            log.warn("Implementation {} is not compatible with service {}", implementation.getName(), service.getName());
            return;
        }

        // 由于已经进行了类型检查，这里的转换是安全的
        // Type casting is safe here as we've done the type check
        Class<T> serviceType = (Class<T>) service;
        Class<? extends T> implementationType = (Class<? extends T>) implementation;

        if (multiple) {
            // 指定别名绑定服务
            // Bind service with alias
            bind(serviceType).annotatedWith(Names.named(qualifier))
                    .to(implementationType)
                    .in(Singleton.class);
        }
        else {
            // 绑定服务
            // Bind service
            bind(serviceType).to(implementationType).in(Singleton.class);
        }
    }

    private void validateInjector()
    {
        if (injector == null) {
            throw new IllegalStateException("Injector not set for plugin: " + getName());
        }
    }

    private String readVersionFromManifest()
            throws IOException
    {
        // 优先使用插件类加载器
        // Use plugin class loader first
        ClassLoader loader = pluginClassLoader != null ? pluginClassLoader : getClass().getClassLoader();

        Enumeration<URL> resources = loader.getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            try (InputStream is = resources.nextElement().openStream()) {
                Manifest manifest = new Manifest(is);
                String version = manifest.getMainAttributes().getValue("Implementation-Version");
                if (version != null) {
                    return version;
                }
            }
        }
        return null;
    }

    /**
     * 获取需要加载的服务类型列表
     * Get list of service types to load
     *
     * @return 服务类型列表
     * list of service types
     */
    public Set<Class<? extends Service>> getServiceTypes()
    {
        return Set.of(Service.class);
    }

    @Override
    protected void configure()
    {
        try {
            // 确保使用插件的类加载器
            // Ensure using plugin's class loader
            if (pluginClassLoader != null) {
                PluginContextManager.runWithClassLoader(pluginClassLoader, () -> {
                    configureServices();
                    return null;
                });
            }
            else {
                log.warn("Plugin class loader not set, using current context class loader");
                configureServices();
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to configure plugin", e);
        }
    }

    /**
     * 配置服务
     * Configure services
     */
    private void configureServices()
    {
        getServiceTypes().forEach(serviceType -> {
            // 获取包路径,默认使用插件类所在包
            // Get package path, default to the plugin's package
            String basePackage = this.getClass().getPackage().getName();

            // 同时使用SPI和注解两种方式加载服务
            // Load services using both SPI and annotation methods
            ServiceBindings bindings;
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (pluginClassLoader != null) {
                bindings = ServiceSpiLoader.loadServices(serviceType, basePackage, pluginClassLoader);
            }
            else {
                bindings = ServiceSpiLoader.loadServices(serviceType, basePackage, contextClassLoader);
            }

            // 支持多个实现
            // Support multiple implementations
            bindings.getBindings().forEach((service, implementation) -> {
                // 使用命名绑定来支持多个实现
                // Use named binding to support multiple implementations
                String implName = implementation.getSimpleName();
                bindService(service, implementation, true, implName);

                // 默认绑定第一个实现
                // Default binding first implementation
                if (!binds.containsKey(service)) {
                    bindService(service, implementation, false, null);
                    binds.put(service, true);
                }
            });
        });

        // 调用子类的配置方法
        // Call subclass configuration method
        configurePlug();
    }

    protected void configurePlug() {}

    public String getName()
    {
        return StringUtils.remove(this.getClass().getSimpleName(), "Plugin");
    }

    /**
     * 声明该插件可配置的字段（名称 / 类型 / 默认值 / 是否允许普通用户临时覆盖）。
     * 默认空：旧插件零改动。Executor / Scheduler / FS / Notify 等子类按需重写。
     *
     * Declare configurable fields exposed by this plugin. Default empty for full
     * backward compatibility — subclasses (Executor / Scheduler / FS / Notify / ...) override as needed.
     */
    public java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField> runtimeConfigures()
    {
        return java.util.Collections.emptyList();
    }

    /**
     * 声明该插件的数据源表单配置字段（字段名 / 类型 / 必填 / 默认值 / 分组 / pipelines 等）。
     * 连接器插件默认返回通用 JDBC 字段（原 default.yaml 语义），转换等其余类型返回 null。
     * 连接器插件应覆写本方法以提供专属配置。
     *
     * Declare the source form configuration of this plugin (field names / types / required /
     * defaults / groups / pipelines). Connector plugins default to the generic JDBC fields
     * (former default.yaml semantics); other plugin types return null. Connector plugins
     * should override this method to provide their own configuration.
     */
    public org.devlive.metaforge.plugin.configure.PluginConfigure describe()
    {
        return getType() == PluginType.CONNECTOR ? org.devlive.metaforge.plugin.configure.DefaultJdbcConfigure.INSTANCE : null;
    }

    public String getVersion()
    {
        if (cachedVersion != null) {
            return cachedVersion;
        }

        // 优先从插件类加载器获取版本
        // Get version from plugin class loader first
        if (pluginClassLoader != null) {
            String version = pluginClassLoader.getPluginVersion();
            if (version != null) {
                cachedVersion = version;
                return version;
            }
        }

        // 回退到包信息和清单文件
        // Fallback to package info and manifest
        String version = this.getClass().getPackage().getImplementationVersion();
        if (version == null) {
            try {
                version = readVersionFromManifest();
            }
            catch (IOException e) {
                log.warn("Failed to read version from MANIFEST.MF", e);
                version = "unknown";
            }
        }

        cachedVersion = version;
        return version;
    }

    public PluginType getType()
    {
        return PluginType.CONNECTOR;
    }

    /**
     * 获取服务实例
     * Get service instance
     *
     * @param serviceClass 服务类型
     * service class type
     * @return 服务实例
     * service instance
     */
    public <T extends Service> T getService(Class<T> serviceClass)
    {
        validateInjector();

        try {
            // 使用插件的类加载器
            // Use plugin's class loader
            if (pluginClassLoader != null) {
                return PluginContextManager.runWithClassLoader(pluginClassLoader, () ->
                        injector.getInstance(serviceClass));
            }
            else {
                return injector.getInstance(serviceClass);
            }
        }
        catch (Exception e) {
            throw new ServiceNotFoundException(
                    "Service not found for type: " + serviceClass.getName(), e);
        }
    }

    /**
     * 获取指定名称的服务实例
     * Get named service instance
     *
     * @param serviceClass 服务接口类型
     * service interface type
     * @param name 服务名称
     * service name
     * @return 服务实例
     * service instance
     */
    public <T extends Service> T getService(Class<T> serviceClass, String name)
    {
        validateInjector();

        try {
            // 使用插件的类加载器
            // Use plugin's class loader
            if (pluginClassLoader != null) {
                return PluginContextManager.runWithClassLoader(pluginClassLoader, () ->
                        injector.getInstance(Key.get(serviceClass, Names.named(name))));
            }
            else {
                return injector.getInstance(Key.get(serviceClass, Names.named(name)));
            }
        }
        catch (ConfigurationException e) {
            throw new ServiceNotFoundException(
                    String.format("Named service not found - type: %s, name: %s",
                            serviceClass.getName(), name), e);
        }
        catch (Exception e) {
            throw new ServiceNotFoundException(
                    String.format("Error getting named service - type: %s, name: %s",
                            serviceClass.getName(), name), e);
        }
    }

    // 获取所有服务
// Get all services
    public <T extends Service> Set<T> getAllServices(Class<T> serviceClass)
    {
        validateInjector();

        try {
            // 使用插件的类加载器
            // Use plugin's class loader
            if (pluginClassLoader != null) {
                return PluginContextManager.runWithClassLoader(pluginClassLoader, () -> {
                    Set<T> services = Sets.newHashSet();
                    // 获取包路径,默认使用插件类所在包
                    // Get package path, default to the plugin's package
                    String basePackage = this.getClass().getPackage().getName();
                    ServiceBindings bindings = ServiceSpiLoader.loadServices(
                            serviceClass, basePackage, pluginClassLoader);
                    bindings.getBindings().get(serviceClass).forEach(impl -> {
                        String name = impl.getSimpleName();
                        services.add(getService(serviceClass, name));
                    });
                    return services;
                });
            }
            else {
                Set<T> services = Sets.newHashSet();
                // 获取包路径,默认使用插件类所在包
                // Get package path, default to the plugin's package
                String basePackage = this.getClass().getPackage().getName();
                ServiceBindings bindings = ServiceSpiLoader.loadServices(
                        serviceClass, basePackage, Thread.currentThread().getContextClassLoader());
                bindings.getBindings().get(serviceClass).forEach(impl -> {
                    String name = impl.getSimpleName();
                    services.add(getService(serviceClass, name));
                });
                return services;
            }
        }
        catch (Exception e) {
            throw new ServiceNotFoundException(
                    "Error getting all services for type: " + serviceClass.getName(), e);
        }
    }
}
