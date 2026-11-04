package org.devlive.metaforge.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.service.service.ReportShareService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves the web UI document for shared reports on real paths, so each response can carry its own
 * framing policy: /share/report/{token} stays same-origin only, /embed/report/{token} may be framed
 * by the sites the owner allowed (CSP frame-ancestors). The web app routes both to the public view.
 */
@Slf4j
@Controller
public class SharedReportPageController
{
    private static final String[] INDEX = {"resources/index.html", "static/index.html", "public/index.html"};

    private final ReportShareService service;

    public SharedReportPageController(ReportShareService service)
    {
        this.service = service;
    }

    @GetMapping("/share/report/{token}")
    public ResponseEntity<String> share(@PathVariable("token") String token)
    {
        return page(HttpStatus.OK, "frame-ancestors 'self'");
    }

    @GetMapping("/embed/report/{token}")
    public ResponseEntity<String> embed(@PathVariable("token") String token)
    {
        // Unknown, private or non-embeddable reports render the "not available" view and refuse framing.
        return page(HttpStatus.OK, "frame-ancestors " + service.frameAncestors(token).orElse("'none'"));
    }

    private ResponseEntity<String> page(HttpStatus status, String frameAncestors)
    {
        for (String location : INDEX) {
            ClassPathResource resource = new ClassPathResource(location);
            if (!resource.exists()) {
                continue;
            }
            try (InputStream stream = resource.getInputStream()) {
                return ResponseEntity.status(status)
                        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                        .cacheControl(CacheControl.noStore())
                        .header("Content-Security-Policy", frameAncestors)
                        .body(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
            catch (IOException e) {
                log.warn("Cannot read the web UI document {}", location, e);
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body("The web UI is not packaged with this server");
    }
}
