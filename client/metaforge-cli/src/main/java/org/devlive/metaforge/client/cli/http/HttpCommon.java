package org.devlive.metaforge.client.cli.http;

import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.devlive.metaforge.client.cli.response.CredentialsResponse;
import org.devlive.metaforge.client.cli.response.ServerResponse;

import java.util.HashMap;
import java.util.Map;

@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "DM_DEFAULT_ENCODING"})
public class HttpCommon
{
    private final HttpConfigure configure;
    private final OkHttpClient client;

    public HttpCommon(HttpConfigure configure)
    {
        this.configure = new HttpConfigure(configure.getHost(), configure.getPort(), configure.getUsername(), configure.getPassword(), configure.getToken(), configure.getMethod(),
                configure.getProtocol(), configure.getUrl(), configure.getSourceId());
        this.client = new OkHttpClient()
                .newBuilder()
                .build();
    }

    private String getUrl()
    {
        return String.format("%s://%s:%s", this.configure.getProtocol(), this.configure.getHost(), this.configure.getPort()) + this.configure.getUrl();
    }

    public CredentialsResponse withCredentials()
    {
        CredentialsResponse httpResponse = new CredentialsResponse();
        Map<String, String> map = new HashMap<>();
        map.put("username", this.configure.getUsername());
        map.put("password", this.configure.getPassword());
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(map));

        Request request = new Request.Builder().url(String.format("%s://%s:%s", this.configure.getProtocol(), this.configure.getHost(), this.configure.getPort()) + this.configure.getUrl()).post(body).build();

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                ServerResponse serverResponse = new Gson().fromJson(response.body().string(), ServerResponse.class);
                if (serverResponse.isStatus()) {
                    httpResponse.setConnect(true);
                    httpResponse.setData(serverResponse.getData());
                }
                else {
                    httpResponse.setConnect(false);
                    httpResponse.setMessage("Failed to connect to the server, " + serverResponse.getMessage());
                }
            }
            else {
                httpResponse.setConnect(false);
                httpResponse.setMessage("Failed to connect to the server, please check whether the configured connection is correct.");
            }
        }
        catch (Exception exception) {
            httpResponse.setConnect(false);
            httpResponse.setMessage("Failed to connect to the server, " + exception.getMessage());
        }
        finally {
            return httpResponse;
        }
    }

    public ServerResponse withTokenForGet()
    {
        ServerResponse serverResponse = new ServerResponse();
        Request request = new Request.Builder().url(this.getUrl()).addHeader("Authorization", "Bearer " + this.configure.getToken()).get().build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                serverResponse = new Gson().fromJson(response.body().string(), ServerResponse.class);
            }
            else {
                serverResponse.setStatus(false);
                serverResponse.setMessage("Failed to connect to the server, please check whether the configured connection is correct.");
            }
        }
        catch (Exception exception) {
            serverResponse.setStatus(false);
            serverResponse.setMessage("Failed to connect to the server, " + exception.getMessage());
        }
        finally {
            return serverResponse;
        }
    }

    public ServerResponse withTokenForPost(String body)
    {
        ServerResponse serverResponse = new ServerResponse();
        Request request = new Request.Builder()
                .url(this.getUrl())
                .addHeader("Authorization", "Bearer " + this.configure.getToken())
                .post(RequestBody.create(body.getBytes(), MediaType.parse("application/json")))
                .build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                serverResponse = new Gson().fromJson(response.body().string(), ServerResponse.class);
            }
            else {
                serverResponse.setStatus(false);
                serverResponse.setMessage("Failed to connect to the server, please check whether the configured connection is correct.");
            }
        }
        catch (Exception exception) {
            serverResponse.setStatus(false);
            serverResponse.setMessage("Failed to connect to the server, " + exception.getMessage());
        }
        finally {
            return serverResponse;
        }
    }
}
