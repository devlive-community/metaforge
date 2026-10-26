package org.devlive.metaforge.client.cli;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.client.cli.http.HttpConfigure;
import org.devlive.metaforge.client.cli.response.CredentialsResponse;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification = "I prefer to suppress these FindBugs warnings")
public class CliPromptProvider
        implements PromptProvider
{
    private final CacheManager cacheManager;

    public CliPromptProvider(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    @Override
    public AttributedString getPrompt()
    {
        String content = "not connected!";
        Cache.ValueWrapper valueWrapper = this.cacheManager.getCache(Support.cache).get(Support.credentials);
        if (ObjectUtils.isNotEmpty(valueWrapper)) {
            CredentialsResponse credentialsResponse = (CredentialsResponse) valueWrapper.get();
            if (credentialsResponse.isConnect()) {
                HttpConfigure configure = (HttpConfigure) this.cacheManager.getCache(Support.cache).get(Support.configure).get();
                content = configure.getHost();
                if (ObjectUtils.isNotEmpty(configure.getSourceId())) {
                    content = String.format("%s:%s", content, configure.getSourceId());
                }
            }
        }
        return new AttributedString(content + ":>", AttributedStyle.DEFAULT);
    }
}
