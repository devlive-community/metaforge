package org.devlive.metaforge.plugin.natived.alioss;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.adapter.Adapter;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementDownload;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.Set;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class AliOssService
        implements PluginService
{
    private AliossConnection connection;
    private Response response;

    @Override
    public ManagementCapabilities managementCapabilities()
    {
        return new ManagementCapabilities("storage", Set.of("namespace:list", "object:list", "object:read", "object:download", "tab:objects", "tab:sql", "query:execute"));
    }

    @Override
    public ManagementPage managementNamespaces(RuntimeConfigure configure, ManagementRequest request)
    {
        return new OssManagement().namespaces(configure, request);
    }

    @Override
    public ManagementPage managementList(RuntimeConfigure configure, ManagementRequest request)
    {
        return new OssManagement().list(configure, request);
    }

    @Override
    public ManagementDetail managementDetail(RuntimeConfigure configure, ManagementRequest request)
    {
        return new OssManagement().detail(configure, request);
    }

    @Override
    public ManagementDownload managementDownload(RuntimeConfigure configure, ManagementRequest request)
    {
        return new OssManagement().download(configure, request);
    }

    @Override
    public String validator()
    {
        return "SELECT * FROM default";
    }

    @Override
    public void connect(RuntimeConfigure configure)
    {
        try {
            this.response = new Response();
            this.connection = new AliossConnection(configure, this.response);
        }
        catch (Exception ex) {
            this.response.setIsConnected(Boolean.FALSE);
            this.response.setMessage(ex.getMessage());
        }
    }

    @Override
    public Response execute(String content)
    {
        if (ObjectUtils.isNotEmpty(this.connection)) {
            log.info("Execute alioss plugin logic started");
            this.response = this.connection.getResponse();
            Adapter processor = new AliossAdapter(this.connection, new AliossParser(content));
            this.response = processor.handlerExecute(content);
            log.info("Execute alioss plugin logic end");
        }
        return this.response;
    }

    @Override
    public Response execute(RuntimeConfigure configure, String content)
    {
        connect(configure);
        return execute(content);
    }

    @Override
    public void destroy()
    {
        if (ObjectUtils.isNotEmpty(this.connection)) {
            this.connection.destroy();
        }
    }
}
