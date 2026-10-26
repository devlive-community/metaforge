package org.devlive.metaforge.captcha.entity;

import lombok.Data;

@Data
public class ResultEntity
        implements Cloneable
{
    // Calculation formula
    private String[] expression;
    // Calculated result
    private Integer result;
    // Image object
    private String image;

    public String[] getExpression()
    {
        return expression.clone();
    }

    public void setExpression(String[] expression)
    {
        if (expression != null) {
            this.expression = expression.clone();
        }
    }

    @Override
    public ResultEntity clone()
    {
        try {
            return (ResultEntity) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
