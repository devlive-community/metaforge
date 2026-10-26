package org.devlive.metaforge.plugin.natived.redis;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.utils.OptionalUtils;
import org.devlive.metaforge.spi.connection.Connection;
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;
import redis.clients.jedis.Jedis;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class RedisConnection
        extends Connection
{
    private Configure configure;
    private Response response;
    private Jedis jedis;

    public RedisConnection(Configure configure, Response response)
    {
        super(configure, response);
    }

    @Override
    protected java.sql.Connection openConnection()
    {
        try {
            this.configure = getConfigure();
            this.response = getResponse();
            this.jedis = new Jedis(this.configure.getHost(), this.configure.getPort());
            if (OptionalUtils.isNotEmpty(this.configure.getUsername()) && OptionalUtils.isNotEmpty(this.configure.getPassword())) {
                this.jedis.auth(this.configure.getUsername().get(), this.configure.getPassword().get());
            }
            if (OptionalUtils.isNotEmpty(this.configure.getPassword())) {
                this.jedis.auth(this.configure.getPassword().get());
            }
            this.jedis.ping("MetaForge");
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
        this.jedis.close();
        log.info("Connection close successful");
    }

    public Jedis getJedis()
    {
        return jedis;
    }
}
