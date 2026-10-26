package org.devlive.metaforge.captcha;

import org.devlive.metaforge.captcha.entity.ResultEntity;
import org.junit.Assert;
import org.junit.Test;

public class CalculateUtilsTest
{
    @Test
    public void testExpressionIsReadable()
    {
        // issue #863：乘号必须是全高的 ×，不允许再出现细小的 *
        for (int i = 0; i < 200; i++) {
            ResultEntity entity = CalculateUtils.generate();
            Assert.assertNotNull(entity.getImage());

            String[] expression = entity.getExpression();
            Assert.assertEquals(5, expression.length);
            Assert.assertEquals("=", expression[3]);
            Assert.assertEquals("?", expression[4]);

            String operator = expression[1];
            Assert.assertTrue("unexpected operator: " + operator,
                    "+".equals(operator) || "-".equals(operator)
                            || "×".equals(operator) || "÷".equals(operator));

            int first = Integer.parseInt(expression[0]);
            int last = Integer.parseInt(expression[2]);
            if ("×".equals(operator)) {
                Assert.assertEquals(first * last, entity.getResult().intValue());
            }
        }
    }
}
