package org.devlive.metaforge.security;

import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

/**
 * A browser asking for a page of the web UI. The UI uses history routing (/query, /admin/user ...),
 * so every such address returns the UI document and the app routes it; the page itself needs no
 * sign-in, the APIs it calls do. API paths, static files and anything with a file extension are not
 * pages.
 */
public class WebUiRequestMatcher
        implements RequestMatcher
{
    private static final String[] NOT_PAGES = {"/api/", "/assets/", "/static/", "/fonts/", "/upload/", "/h2-console", "/share/", "/embed/"};

    @Override
    public boolean matches(HttpServletRequest request)
    {
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            return false;
        }
        return isPage(path(request));
    }

    public static boolean isPage(String path)
    {
        if (path == null || path.isEmpty() || !path.startsWith("/") || "/api".equals(path)) {
            return false;
        }
        for (String prefix : NOT_PAGES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        // Files carry an extension in their last segment (favicon.ico, robots.txt, index.html).
        String last = path.substring(path.lastIndexOf('/') + 1);
        return !last.contains(".");
    }

    static String path(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context != null && !context.isEmpty() && uri.startsWith(context) ? uri.substring(context.length()) : uri;
    }
}
