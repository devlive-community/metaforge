package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.junit.Assert;
import org.junit.Test;

public class ManagementCapabilitiesTest
{
    @Test
    public void mysqlAdvertisesImplementedDdlAndTabs()
    {
        ManagementCapabilities capabilities = ManagementCapabilities.forJdbc("mysql");
        Assert.assertTrue(capabilities.supports("table:create"));
        Assert.assertTrue(capabilities.supports("column:modify"));
        Assert.assertTrue(capabilities.supports("tab:structure"));
        Assert.assertTrue(capabilities.supports("tab:data"));
        capabilities.getCapabilities().forEach(capability -> Assert.assertTrue(capability.matches("[a-z-]+:[a-z-]+")));
    }

    @Test
    public void otherJdbcDialectsDoNotAdvertiseMysqlGenerators()
    {
        for (String type : new String[] {"postgresql", "sqlserver", "oracle", "dm"}) {
            ManagementCapabilities capabilities = ManagementCapabilities.forJdbc(type);
            Assert.assertTrue(capabilities.supports("column:read"));
            Assert.assertTrue(capabilities.supports("tab:structure"));
            Assert.assertFalse(capabilities.supports("column:modify"));
            Assert.assertFalse(capabilities.supports("table:create"));
            Assert.assertFalse(capabilities.supports("tab:statement"));
        }
    }

    @Test
    public void nativeFamiliesNeverInheritRelationalOperations()
    {
        Assert.assertEquals("key-value", ManagementCapabilities.forJdbc("redis").getKind());
        Assert.assertEquals("document", ManagementCapabilities.forJdbc("mongodb").getKind());
        ManagementCapabilities storage = ManagementCapabilities.queryOnly("storage");
        Assert.assertTrue(storage.supports("tab:sql"));
        Assert.assertFalse(storage.supports("tab:structure"));
        Assert.assertFalse(storage.supports("column:modify"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void callersCannotMutateAPluginCapabilitySet()
    {
        ManagementCapabilities.forJdbc("mysql").getCapabilities().clear();
    }
}
