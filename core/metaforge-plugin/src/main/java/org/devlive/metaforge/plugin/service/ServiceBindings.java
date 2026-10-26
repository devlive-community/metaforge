package org.devlive.metaforge.plugin.service;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.plugin.Service;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class ServiceBindings
{
    // 限制绑定类型必须是 Service 的子类
    // Restrict binding types to Service subclasses
    private final Multimap<Class<? extends Service>, Class<? extends Service>> bindings = LinkedListMultimap.create();

    /**
     * 添加服务绑定
     * Add service binding
     *
     * @param serviceType 服务类型（必须继承自Service）
     * service type (must extend Service)
     * @param implementationType 实现类型（必须继承自Service）
     * implementation type (must extend Service)
     */
    public void addBinding(Class<? extends Service> serviceType, Class<? extends Service> implementationType)
    {
        bindings.put(serviceType, implementationType);
    }

    /**
     * 获取所有绑定
     * Get all bindings
     *
     * @return 服务绑定映射
     * service binding mapping
     */
    public Multimap<Class<? extends Service>, Class<? extends Service>> getBindings()
    {
        return bindings;
    }
}
