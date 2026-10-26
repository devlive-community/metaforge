package org.devlive.metaforge.common;

import org.devlive.metaforge.common.utils.OptionalUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class OptionalUtilsTest
{
    private Optional<String> empty = Optional.of("");
    private Optional<String> notEmpty = Optional.of("s");

    @Test
    public void isEmpty()
    {
        Assert.assertFalse(OptionalUtils.isEmpty(empty));
        Assert.assertFalse(OptionalUtils.isEmpty(notEmpty));
    }

    @Test
    public void isNotEmpty()
    {
        Assert.assertTrue(OptionalUtils.isNotEmpty(empty));
        Assert.assertTrue(OptionalUtils.isNotEmpty(notEmpty));
    }
}
