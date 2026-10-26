package org.devlive.metaforge.service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.devlive.metaforge.service.annotation.DynamicJsonView;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
@Aspect
@Component
@Order(1)
public class DynamicJsonViewAspect
{
    @Around("@annotation(dynamicJsonView)")
    public Object around(ProceedingJoinPoint point, DynamicJsonView dynamicJsonView)
            throws Throwable
    {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();

        log.debug("Executing DynamicJsonViewAspect");

        // 获取当前用户权限
        // Get the current user permissions
        UserEntity entity = UserDetailsService.getUser();
        long count = entity.getRoles()
                .stream()
                .filter(v -> v.getCode().contains("ADMIN"))
                .count();
        boolean isAdmin = count > 0;

        // 设置对应的 JsonView
        // Set the corresponding JsonView
        Class<?> viewClass = isAdmin ? dynamicJsonView.admin() : dynamicJsonView.user();

        log.debug("Setting JsonView to: {}", viewClass);

        attributes.setAttribute("JSON_VIEW", viewClass, RequestAttributes.SCOPE_REQUEST);

        // 执行原方法
        // Execute the original method
        return point.proceed();
    }
}
