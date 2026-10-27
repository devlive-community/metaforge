package org.devlive.metaforge.security;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/** Keep administrative APIs protected independently of browser URLs and cached menus. */
public final class AdminApiRequestMatcher
        implements RequestMatcher
{
    private final List<RequestMatcher> matchers = new ArrayList<>();

    public AdminApiRequestMatcher()
    {
        for (String path : new String[] {"/api/v1/admin/**", "/api/v1/role/**", "/api/v1/menu/**", "/api/v1/schedule/**", "/api/v1/configure/runtime/**"}) {
            matchers.add(new AntPathRequestMatcher(path));
        }
        add("POST", "/api/v1/user", "/api/v1/user/list", "/api/v1/function", "/api/v1/function/list");
        add("PUT", "/api/v1/user", "/api/v1/user/allocationRole", "/api/v1/function", "/api/v1/function/import");
        add("DELETE", "/api/v1/user", "/api/v1/user/**", "/api/v1/function", "/api/v1/function/**");
        add("GET", "/api/v1/user/info/**");
    }

    private void add(String method, String... paths)
    {
        for (String path : paths) {
            matchers.add(new AntPathRequestMatcher(path, method));
            if (!path.endsWith("/**")) {
                matchers.add(new AntPathRequestMatcher(path + "/", method));
            }
        }
    }

    @Override
    public boolean matches(HttpServletRequest request)
    {
        return matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }
}
