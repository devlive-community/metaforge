package org.devlive.metaforge.spi.connection.http;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.devlive.metaforge.spi.connection.HttpConfigure;

import java.net.ConnectException;

import static com.google.common.base.Preconditions.checkArgument;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class HttpRetryInterceptor
        implements Interceptor
{
    private final HttpConfigure configure;
    private int count = 1;

    public HttpRetryInterceptor(HttpConfigure configure)
    {
        this.configure = configure;
    }

    @Override
    public Response intercept(Chain chain)
    {
        return retry(chain);
    }

    public Response retry(Chain chain)
    {
        Response response = null;
        Request request = chain.request();
        try {
            Thread.sleep(2000);
            response = chain.proceed(request);
            while (!response.isSuccessful() && count < configure.getRetry()) {
                count++;
                response = retry(chain);
            }
        }
        catch (Exception e) {
            if (e instanceof ConnectException) {
                checkArgument(false, e.getMessage());
            }
            else {
                while (count < configure.getRetry()) {
                    count++;
                    response = retry(chain);
                }
            }
        }
        return response;
    }
}
