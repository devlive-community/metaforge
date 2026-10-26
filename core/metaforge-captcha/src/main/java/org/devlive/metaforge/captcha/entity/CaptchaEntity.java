package org.devlive.metaforge.captcha.entity;

public class CaptchaEntity
{
    // The first count: output data
    private int firstNumber = 0;
    // The second count: output data
    private int lastNumber = 0;
    // Calculation method: 0 addition, 1 subtraction, 2 multiplication, 3 division, 4 equal
    private int calc = 0;
    // The type of data to be taken, 0 Arabic numerals | 1 Chinese uppercase Chinese characters
    private int number = 0;

    public CaptchaEntity()
    {
    }

    public CaptchaEntity(int firstNumber, int lastNumber, int calc, int number)
    {
        super();
        this.firstNumber = firstNumber;
        this.lastNumber = lastNumber;
        this.calc = calc;
        this.number = number;
    }

    public int getFirstNumber()
    {
        return firstNumber;
    }

    public void setFirstNumber(int firstNumber)
    {
        this.firstNumber = firstNumber;
    }

    public int getLastNumber()
    {
        return lastNumber;
    }

    public void setLastNumber(int lastNumber)
    {
        this.lastNumber = lastNumber;
    }

    public int getCalc()
    {
        return calc;
    }

    public void setCalc(int calc)
    {
        this.calc = calc;
    }

    public int getNumber()
    {
        return number;
    }

    public void setNumber(int number)
    {
        this.number = number;
    }

    public String toString()
    {
        return "firstNumber = " + firstNumber + " ,lastNumber = " + lastNumber + " ,calc = " + calc + " ,number = " + number;
    }
}
