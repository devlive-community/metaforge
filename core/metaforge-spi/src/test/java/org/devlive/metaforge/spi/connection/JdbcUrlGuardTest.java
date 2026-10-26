package org.devlive.metaforge.spi.connection;

import org.junit.Assert;
import org.junit.Test;

public class JdbcUrlGuardTest
{
    @Test
    public void testValidHostAndDatabase()
    {
        Assert.assertEquals("127.0.0.1", JdbcUrlGuard.validateHost("127.0.0.1"));
        Assert.assertEquals("[::1]", JdbcUrlGuard.validateHost("[::1]"));
        JdbcUrlGuard.validateDatabase("testdb");
        JdbcUrlGuard.validateDatabase("test_db.v2");
        JdbcUrlGuard.validateDatabase("测试库");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testH2InitInjectionViaDatabase()
    {
        JdbcUrlGuard.validateDatabase("testdb;INIT=CREATE ALIAS EXEC AS 'void cmd_exec(String cmd) throws java.lang.Exception {Runtime.getRuntime().exec(cmd)\\;}'\\;CALL EXEC ('touch /tmp/pwned')\\;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHostWithParameterSeparator()
    {
        JdbcUrlGuard.validateHost("127.0.0.1?allowLoadLocalInfile=true");
    }

    @Test
    public void testSafeParameters()
    {
        JdbcUrlGuard.validateParameter("useSSL", "false");
        JdbcUrlGuard.validateParameter("serverTimezone", "Asia/Shanghai");
        JdbcUrlGuard.validateParameter("characterEncoding", "utf8");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForbiddenParameter()
    {
        JdbcUrlGuard.validateParameter("allow_load_local_infile", "true");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParameterValueInjection()
    {
        JdbcUrlGuard.validateParameter("serverTimezone", "UTC?allowLoadLocalInfile=true");
    }
}
