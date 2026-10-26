package org.devlive.metaforge.common.utils;

import org.junit.Assert;
import org.junit.Test;

public class SignUtilsTest
{
    @Test
    public void testSign()
            throws Exception
    {
        Assert.assertNotNull(SignUtils.sign("SSSS")
                .getSign());
    }
}
