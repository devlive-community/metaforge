package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PluginServiceTest
{
    private Configure configure()
    {
        Configure configure = new Configure();
        configure.setType("mysql");
        configure.setHost("127.0.0.1");
        configure.setPort(3306);
        configure.setDatabase(Optional.of("test"));
        return configure;
    }

    @Test
    public void testBuildUrl()
    {
        Assert.assertEquals("jdbc:mysql://127.0.0.1:3306/test", new PluginService()
        {
        }.url(configure()));
    }

    @Test
    public void testBuildUrlWithSsl()
    {
        Configure configure = configure();
        configure.setSsl(Optional.of(true));
        Assert.assertEquals("jdbc:mysql://127.0.0.1:3306/test?ssl=true", new PluginService()
        {
        }.url(configure));
    }

    @Test
    public void testBuildUrlWithEnv()
    {
        Configure configure = configure();
        Map<String, Object> env = new HashMap<>();
        env.put("serverTimezone", "Asia/Shanghai");
        configure.setEnv(Optional.of(env));
        Assert.assertEquals("jdbc:mysql://127.0.0.1:3306/test?serverTimezone=Asia/Shanghai", new PluginService()
        {
        }.url(configure));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildUrlRejectsH2InitInjectionViaDatabase()
    {
        // issue #966 的 H2 RCE 载荷：通过 database 注入 INIT 参数
        Configure configure = configure();
        configure.setDatabase(Optional.of("testdb;INIT=CREATE ALIAS EXEC AS 'void cmd_exec(String cmd) throws java.lang.Exception {Runtime.getRuntime().exec(cmd)\\;}'\\;CALL EXEC ('touch /tmp/pwned')\\;"));
        new PluginService()
        {
        }.url(configure);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildUrlRejectsInjectionViaHost()
    {
        Configure configure = configure();
        configure.setHost("127.0.0.1?allowLoadLocalInfile=true");
        new PluginService()
        {
        }.url(configure);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildUrlRejectsForbiddenEnvParameter()
    {
        // issue #966 的 MySQL 任意文件读取载荷：allowLoadLocalInfile 参数
        Configure configure = configure();
        Map<String, Object> env = new HashMap<>();
        env.put("allow_load_local_infile", "true");
        configure.setEnv(Optional.of(env));
        new PluginService()
        {
        }.url(configure);
    }

    @Test
    public void testGetDatabasesPassesThroughSuccessfulResponse()
    {
        Response expected = Response.builder()
                .isConnected(Boolean.TRUE)
                .isSuccessful(Boolean.TRUE)
                .build();
        PluginService service = new PluginService()
        {
            @Override
            public Response execute(Configure configure, String content)
            {
                return expected;
            }
        };
        Assert.assertSame(expected, service.getDatabases(configure()));
    }

    @Test
    public void testGetDatabasesFallsBackWhenSqlFails()
    {
        // SQL 执行失败（连接正常）时必须走 JDBC 元数据回退，而不是原样返回失败
        PluginService service = new PluginService()
        {
            @Override
            public Response execute(Configure configure, String content)
            {
                return Response.builder()
                        .isConnected(Boolean.TRUE)
                        .isSuccessful(Boolean.FALSE)
                        .message("sql failed")
                        .build();
            }
        };
        Response response = service.getDatabases(configure());
        Assert.assertEquals(Boolean.FALSE, response.getIsSuccessful());
        // 测试环境没有真实数据源，回退在打开 JDBC 连接时按预期终止
        Assert.assertEquals("Connection is not available or not a JDBC connection", response.getMessage());
    }

    @Test
    public void testGetDatabasesKeepsResponseWhenConnectionFailed()
    {
        // 连接本身失败时不触发回退，原样返回连接失败信息
        Response expected = Response.builder()
                .isConnected(Boolean.FALSE)
                .isSuccessful(Boolean.FALSE)
                .message("connect refused")
                .build();
        PluginService service = new PluginService()
        {
            @Override
            public Response execute(Configure configure, String content)
            {
                return expected;
            }
        };
        Assert.assertSame(expected, service.getDatabases(configure()));
    }

    @Test
    public void testGetTablesFallsBackWhenSqlFails()
    {
        PluginService service = new PluginService()
        {
            @Override
            public Response execute(Configure configure, String content)
            {
                return Response.builder()
                        .isConnected(Boolean.TRUE)
                        .isSuccessful(Boolean.FALSE)
                        .message("sql failed")
                        .build();
            }
        };
        Response response = service.getTables(configure(), "test");
        Assert.assertEquals(Boolean.FALSE, response.getIsSuccessful());
        Assert.assertEquals("Connection is not available or not a JDBC connection", response.getMessage());
    }

    @Test
    public void testGetColumnsFallsBackWhenSqlFails()
    {
        PluginService service = new PluginService()
        {
            @Override
            public Response execute(Configure configure, String content)
            {
                return Response.builder()
                        .isConnected(Boolean.TRUE)
                        .isSuccessful(Boolean.FALSE)
                        .message("sql failed")
                        .build();
            }
        };
        Response response = service.getColumns(configure(), "test", "t");
        Assert.assertEquals(Boolean.FALSE, response.getIsSuccessful());
        Assert.assertEquals("Connection is not available or not a JDBC connection", response.getMessage());
    }
}
