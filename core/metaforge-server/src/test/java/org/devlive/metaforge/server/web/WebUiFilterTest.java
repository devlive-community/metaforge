package org.devlive.metaforge.server.web;

import org.devlive.metaforge.security.WebUiRequestMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebUiFilterTest
{
    @Test
    void pagesAreUiAddressesOnly()
    {
        for (String page : new String[] {"/", "/query", "/admin/user", "/auth/oauth", "/dataset/adhoc/a/b"}) {
            assertTrue(WebUiRequestMatcher.isPage(page), page);
        }
        for (String other : new String[] {"/api/v1/user", "/api", "/assets/index-1.js", "/static/images/a.png", "/favicon.ico", "/robots.txt",
                "/share/report/abc", "/embed/report/abc", "/upload/a", "/h2-console", "/fonts/x"}) {
            assertFalse(WebUiRequestMatcher.isPage(other), other);
        }
    }

    private static WebUiDocument document(String html)
    {
        return new WebUiDocument()
        {
            @Override
            public Optional<String> html()
            {
                return Optional.ofNullable(html);
            }
        };
    }

    private static MockHttpServletRequest request(String method, String path, String accept)
    {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (accept != null) {
            request.addHeader("Accept", accept);
        }
        return request;
    }

    @Test
    void browserPageRequestsGetTheUiDocument()
            throws Exception
    {
        WebUiFilter filter = new WebUiFilter(document("<!doctype html><title>MetaForge</title>"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("GET", "/admin/user", "text/html,application/xhtml+xml"), response, chain);
        assertEquals(200, response.getStatus());
        assertEquals("<!doctype html><title>MetaForge</title>", response.getContentAsString());
        assertTrue(response.getContentType().startsWith("text/html"));
        assertEquals("no-cache", response.getHeader("Cache-Control"));
        assertNull(chain.getRequest(), "a served page does not continue down the chain");
    }

    @Test
    void apisFilesAndNonBrowserRequestsPassThrough()
            throws Exception
    {
        WebUiFilter filter = new WebUiFilter(document("<html/>"));
        for (MockHttpServletRequest request : new MockHttpServletRequest[] {
                request("GET", "/api/v1/user", "text/html"),
                request("GET", "/assets/app.js", "text/html"),
                request("POST", "/query", "text/html"),
                request("GET", "/query", "application/json"),
                request("GET", "/query", null)}) {
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(request, new MockHttpServletResponse(), chain);
            assertNotNull(chain.getRequest(), request.getMethod() + " " + request.getRequestURI());
        }
        MockFilterChain chain = new MockFilterChain();
        new WebUiFilter(document(null)).doFilter(request("GET", "/query", "text/html"), new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest(), "without a packaged UI the request continues");
    }
}
