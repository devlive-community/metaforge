package org.devlive.metaforge.server.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** The web UI document (index.html) packaged with the server, read once. */
@Slf4j
@Component
public class WebUiDocument
{
    private static final String[] LOCATIONS = {"resources/index.html", "static/index.html", "public/index.html"};

    private volatile String html;

    public Optional<String> html()
    {
        String cached = html;
        if (cached != null) {
            return Optional.of(cached);
        }
        for (String location : LOCATIONS) {
            ClassPathResource resource = new ClassPathResource(location);
            if (!resource.exists()) {
                continue;
            }
            try (InputStream stream = resource.getInputStream()) {
                html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                return Optional.of(html);
            }
            catch (IOException e) {
                log.warn("Cannot read the web UI document {}", location, e);
            }
        }
        return Optional.empty();
    }
}
