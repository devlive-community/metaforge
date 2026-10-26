package org.devlive.metaforge.common.enums;

public enum State
{
    SUCCESS(200, "Query successful"),
    FAILURE(400, "Query failure");
    private Integer code;
    private String value;

    State(Integer code, String value)
    {
        this.code = code;
        this.value = value;
    }

    public Integer getCode()
    {
        return code;
    }

    public String getValue()
    {
        return value;
    }
}
