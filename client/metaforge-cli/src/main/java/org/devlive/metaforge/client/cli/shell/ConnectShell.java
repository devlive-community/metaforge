package org.devlive.metaforge.client.cli.shell;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.client.cli.Support;
import org.devlive.metaforge.client.cli.http.HttpCommon;
import org.devlive.metaforge.client.cli.http.HttpConfigure;
import org.devlive.metaforge.client.cli.http.HttpMethod;
import org.devlive.metaforge.client.cli.response.CredentialsResponse;
import org.springframework.cache.CacheManager;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@ShellCommandGroup(value = "Server")
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification = "I prefer to suppress these FindBugs warnings")
public class ConnectShell
{
    private final CacheManager cacheManager;

    public ConnectShell(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    @ShellMethod(value = "Connect to remote server", key = {"connect"}, prefix = "-")
    public void connect(@ShellOption(value = {"--host", "-h"}) String host,
            @ShellOption(value = {"--port", "-p"}) int port,
            @ShellOption(value = {"--username", "-u"}) String username,
            @ShellOption(value = {"--password", "-P"}) String password)
    {
        HttpConfigure configure = new HttpConfigure();
        configure.setHost(host);
        configure.setPort(port);
        configure.setMethod(HttpMethod.POST);
        configure.setUsername(username);
        configure.setPassword(password);
        configure.setUrl("/api/auth/signin");
        HttpCommon httpCommon = new HttpCommon(configure);
        CredentialsResponse httpResponse = httpCommon.withCredentials();
        if (httpResponse.isConnect()) {
            cacheManager.getCache(Support.cache).put(Support.credentials, httpResponse);
            cacheManager.getCache(Support.cache).put(Support.configure, configure);
            System.out.println("Connect successful! Welcome to MetaForge");
        }
        else {
            System.out.println(httpResponse.getMessage());
        }
    }
}
