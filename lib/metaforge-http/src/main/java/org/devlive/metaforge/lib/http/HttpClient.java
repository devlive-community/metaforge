package org.devlive.metaforge.lib.http;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", "EI_EXPOSE_REP2"})
public class HttpClient
{
    private static final int CONNECTION_TIME_OUT = 200000;
    private static final int SOCKET_TIME_OUT = 20000;
    private static final int MAX_IDLE_CONNECTIONS = 30;
    private static final long KEEP_ALLIVE_TIME = 60000L;
    private static volatile HttpClient httpClient;
    private final OkHttpClient okHttpClient;
    private final HttpConfigure configure;

    public HttpClient(HttpConfigure configure)
    {
        this.configure = configure;
        ConnectionPool connectionPool = new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALLIVE_TIME, TimeUnit.MILLISECONDS);
        this.okHttpClient = new OkHttpClient().newBuilder()
                .readTimeout(SOCKET_TIME_OUT, TimeUnit.MILLISECONDS)
                .writeTimeout(SOCKET_TIME_OUT, TimeUnit.MILLISECONDS)
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(configure.getAutoConnected())
                .connectTimeout(CONNECTION_TIME_OUT, TimeUnit.MILLISECONDS)
                .addInterceptor(new HttpRetryInterceptor(configure))
                .addNetworkInterceptor(new HttpRetryInterceptor(configure))
                .build();
    }

    public static HttpClient getInstance(HttpConfigure configure)
    {
        httpClient = new HttpClient(configure);
        return httpClient;
    }

    public String execute()
    {
        switch (configure.getMethod()) {
            case GET:
                return this.get();
            case POST:
                return this.post();
            default:
                return null;
        }
    }

    private String get()
    {
        HttpUrl.Builder builder = HttpUrl.parse(this.formatJdbcUrl()).newBuilder();
        if (ObjectUtils.isNotEmpty(configure.getParams())) {
            for (String key : configure.getParams().keySet()) {
                builder.addQueryParameter(key, configure.getParams().get(key));
            }
        }
        log.info("Request url {}", builder.build());
        Request request = new Request.Builder()
                .addHeader("Accept-Encoding", "identity")
                .url(builder.build().toString())
                .build();
        return execute(request);
    }

    private String post()
    {
        RequestBody requestBody = RequestBody.create(configure.getMediaType(), configure.getBody());
        HttpUrl.Builder builder = HttpUrl.parse(this.formatJdbcUrl())
                .newBuilder();

        // Adding Path Parameters
        if (ObjectUtils.isNotEmpty(configure.getParams())) {
            for (String key : configure.getParams().keySet()) {
                builder.addEncodedQueryParameter(key, configure.getParams()
                        .get(key));
            }
        }

        log.info("Request url {}", builder.build());
        Request request = new Request.Builder()
                .post(requestBody)
                .addHeader("Accept-Encoding", "identity")
                .url(builder.build().toString()).build();
        return execute(request);
    }

    private String execute(Request request)
    {
        String responseBody = null;
        try (Response response = okHttpClient.newCall(request)
                .execute()) {
            responseBody = response.body()
                    .string();
        }
        catch (IOException e) {
            log.warn("Request error", e);
        }
        return responseBody;
    }

    private String formatJdbcUrl()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.configure.getProtocol());
        buffer.append("://");
        buffer.append(this.configure.getHost());
        buffer.append(":");
        buffer.append(this.configure.getPort());
        if (StringUtils.isNotEmpty(this.configure.getPath())) {
            buffer.append("/");
            buffer.append(this.configure.getPath());
        }
        return buffer.toString();
    }
}
