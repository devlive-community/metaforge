package org.devlive.metaforge.plugin.natived.alioss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.connection.Connection;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class AliossConnection
        extends Connection
{
    private RuntimeConfigure configure;
    private Response response;

    @Getter
    private OSS ossClient;

    public AliossConnection(RuntimeConfigure configure, Response response)
    {
        super(configure, response);
    }

    @Override
    protected java.sql.Connection openConnection()
    {
        try {
            this.configure = getConfigure();
            this.response = getResponse();
            this.ossClient = new OSSClientBuilder()
                    .build(configure.getHost(), configure.getUsername().get(), configure.getPassword().get());
            response.setIsConnected(Boolean.TRUE);
        }
        catch (Exception ex) {
            log.error("Connection failed ", ex);
            response.setIsConnected(Boolean.FALSE);
            response.setMessage(ex.getMessage());
        }
        return null;
    }

    @Override
    public void destroy()
    {
        this.ossClient.shutdown();
        log.info("Connection close successful");
    }
}
