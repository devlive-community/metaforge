package org.devlive.metaforge.captcha.enums;

public enum NumberEnum
{
    ONE("1", "壹"),
    TWO("2", "贰"),
    THREE("3", "叁"),
    FOUR("4", "肆"),
    FIVE("5", "伍"),
    SIX("6", "陆"),
    SEVEN("7", "柒"),
    EIGHT("8", "捌"),
    NINE("9", "玖"),
    ZERO("0", "零");

    private final String valueEn;
    private final String valueZh;

    NumberEnum(String valueEn, String valueZh)
    {
        this.valueEn = valueEn;
        this.valueZh = valueZh;
    }

    public String getValueEn()
    {
        return valueEn;
    }

    public String getValueZh()
    {
        return valueZh;
    }
}
