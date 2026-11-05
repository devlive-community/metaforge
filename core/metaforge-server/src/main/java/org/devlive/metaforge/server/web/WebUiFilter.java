package org.devlive.metaforge.server.web;

import org.devlive.metaforge.security.WebUiRequestMatcher;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Answers browser page requests (/query, /admin/user ...) with the web UI document, so addresses of
 * the history-routed UI open directly and survive a reload. Everything else continues as usual.
 */
@Component
public class WebUiFilter
        extends OncePerRequestFilter
{
    private final WebUiRequestMatcher pages = new WebUiRequestMatcher();
    private final WebUiDocument document;

    public WebUiFilter(WebUiDocument document)
    {
        this.document = document;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        Optional<String> html = pages.matches(request) && accept != null && accept.contains(MediaType.TEXT_HTML_VALUE) ? document.html() : Optional.empty();
        if (html.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        byte[] body = html.get().getBytes(StandardCharsets.UTF_8);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // The document names hashed assets; it must be revalidated so a deploy shows at once.
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue());
        response.setContentLength(body.length);
        if (!"HEAD".equals(request.getMethod())) {
            response.getOutputStream().write(body);
        }
    }
}
