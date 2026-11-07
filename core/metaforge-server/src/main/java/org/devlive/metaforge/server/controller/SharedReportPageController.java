package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.server.web.WebUiDocument;
import org.devlive.metaforge.service.service.DashboardService;
import org.devlive.metaforge.service.service.ReportShareService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;

/**
 * Serves the web UI document for shared reports on real paths, so each response can carry its own
 * framing policy: /share/report/{token} stays same-origin only, /embed/report/{token} may be framed
 * by the sites the owner allowed (CSP frame-ancestors). The web app routes both to the public view.
 */
@Controller
public class SharedReportPageController
{
    private final ReportShareService service;
    private final DashboardService screens;
    private final WebUiDocument document;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed services are intentionally shared singletons")
    public SharedReportPageController(ReportShareService service, DashboardService screens, WebUiDocument document)
    {
        this.service = service;
        this.screens = screens;
        this.document = document;
    }

    @GetMapping("/share/screen/{token}")
    public ResponseEntity<String> shareScreen(@PathVariable("token") String token)
    {
        return page(HttpStatus.OK, "frame-ancestors 'self'");
    }

    @GetMapping("/embed/screen/{token}")
    public ResponseEntity<String> embedScreen(@PathVariable("token") String token)
    {
        // Unknown, private or non-embeddable screens render the "not available" view and refuse framing.
        return page(HttpStatus.OK, "frame-ancestors " + screens.publicFrameAncestors(token).orElse("'none'"));
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
        return document.html()
                .map(html -> ResponseEntity.status(status)
                        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                        .cacheControl(CacheControl.noStore())
                        .header("Content-Security-Policy", frameAncestors)
                        .body(html))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body("The web UI is not packaged with this server"));
    }
}
