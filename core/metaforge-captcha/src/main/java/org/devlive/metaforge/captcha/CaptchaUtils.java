package org.devlive.metaforge.captcha;

import org.devlive.metaforge.captcha.entity.CaptchaEntity;
import org.devlive.metaforge.captcha.entity.ResultEntity;
import org.devlive.metaforge.captcha.enums.CalculateEnum;

import java.awt.Color;
import java.util.Random;

public class CaptchaUtils
{
    private static final Random RANDOM = new Random();

    private CaptchaUtils()
    {}

    /**
     * Generates a random background color.
     *
     * @return a Color object representing the generated background color
     */
    public static Color getBackgroundColor()
    {
        int rColor = RANDOM.nextInt(256);
        int gColor = RANDOM.nextInt(256);
        int bColor = RANDOM.nextInt(256);
        return new Color(rColor, gColor, bColor);
    }

    /**
     * Generates a random dark color that stays readable on the light background.
     *
     * @return a Color object whose channels are dark enough for foreground text
     */
    public static Color getForegroundColor()
    {
        int rColor = RANDOM.nextInt(128);
        int gColor = RANDOM.nextInt(128);
        int bColor = RANDOM.nextInt(128);
        return new Color(rColor, gColor, bColor);
    }

    /**
     * Generates a CaptchaEntity object with random calculation numbers.
     *
     * @return A CaptchaEntity object with randomly generated firstNumber, lastNumber,
     * calc, and number values.
     */
    private static CaptchaEntity generateCalcNumber()
    {
        int firstNumber;
        int lastNumber;
        int calc;
        int number;
        number = RANDOM.nextInt(2);
        firstNumber = RANDOM.nextInt(10);
        lastNumber = RANDOM.nextInt(10);
        calc = RANDOM.nextInt(4);
        /**
         * When the calculation method is subtraction by 1
         * The minuend cannot be less than the subtrahend, when it is less than, the calculation data will be regenerated 1 - 9
         */
        while (calc == 1 && firstNumber - lastNumber < 0) {
            lastNumber = RANDOM.nextInt(10);
        }
        /**
         * When the calculation method is division by 3
         * > The dividend cannot be 0, when it is 0, the calculation data will be regenerated
         */
        while (calc == 3 && lastNumber == 0) {
            lastNumber = RANDOM.nextInt(10);
        }
        /**
         * When the calculation method is division by 3 and the dividend is greater than 0
         * > When the dividend cannot be divisible by the divisor, regenerate the calculation data
         * > When the divisor is equal to 0, add 1 to the divisor
         */
        while (calc == 3 && (firstNumber % lastNumber) > 0) {
            lastNumber = RANDOM.nextInt(10);
            if (lastNumber == 0) {
                lastNumber = 1;
            }
        }
        CaptchaEntity entity = new CaptchaEntity();
        entity.setFirstNumber(firstNumber);
        entity.setLastNumber(lastNumber);
        entity.setCalc(calc);
        entity.setNumber(number);
        return entity;
    }

    /**
     * Generates a CaptchaEntity object representing a calculated image.
     *
     * @return A CaptchaEntity object containing the calculated image.
     */
    public static ResultEntity generateCalculateImage()
    {
        CaptchaEntity entity = generateCalcNumber();
        int calc = entity.getCalc();
        int firstNumber = entity.getFirstNumber();
        int lastNumber = entity.getLastNumber();
        String _firstNumber = String.valueOf(firstNumber);
        String _lastNumber = String.valueOf(lastNumber);
        String _calc = getCalculateEn(calc, calc);
        String[] result = new String[] {_firstNumber, _calc, _lastNumber, CalculateEnum.EQUAL.getValueEn(), "?"};
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setExpression(result);
        resultEntity.setResult(generateCalculateResult(entity));
        return resultEntity;
    }

    /**
     * Generates the result of a calculation based on the given CaptchaEntity.
     *
     * @param entity the CaptchaEntity containing the calculation details
     * @return the result of the calculation
     */
    private static Integer generateCalculateResult(CaptchaEntity entity)
    {
        int calc = entity.getCalc();
        int firstNumber = entity.getFirstNumber();
        int lastNumber = entity.getLastNumber();
        int result;
        switch (calc) {
            case 0:
                result = firstNumber + lastNumber;
                break;
            case 1:
                result = firstNumber - lastNumber;
                break;
            case 2:
                result = firstNumber * lastNumber;
                break;
            case 3:
                result = firstNumber / lastNumber;
                break;
            default:
                result = 0;
        }
        return result;
    }

    /**
     * Returns the English value of a given calculation method.
     *
     * @param calc the calculation method (0: addition, 1: subtraction, 2: multiplication, 3: division, 4: equal)
     * @param number the number to perform the calculation on
     * @return the English value of the calculation method
     */
    private static String getCalculateEn(int calc, int number)
    {
        String result; // Calculation method: 0 addition, 1 subtraction, 2 multiplication, 3 division, 4 equal
        switch (calc) {
            case 0:
                result = CalculateEnum.ADD.getValueEn();
                break;
            case 1:
                result = CalculateEnum.SUBTRACT.getValueEn();
                break;
            case 2:
                result = CalculateEnum.MULTIPLY.getValueEn();
                break;
            case 3:
                result = CalculateEnum.DIVIDE.getValueEn();
                break;
            case 4:
                result = CalculateEnum.EQUAL.getValueEn();
                break;
            default:
                throw new RuntimeException("Invalid calculation method");
        }
        return result;
    }

    /**
     * Retrieves the corresponding Chinese value based on the calculation type and number.
     *
     * @param calc the calculation type
     * @param number the number
     * @return the corresponding Chinese value
     */
    private static String getCalculateZh(int calc, int number)
    {
        String result;
        switch (calc) {
            case 0:
                result = CalculateEnum.ADD.getValueZh();
                break;
            case 1:
                result = CalculateEnum.SUBTRACT.getValueZh();
                break;
            case 2:
                result = CalculateEnum.MULTIPLY.getValueZh();
                break;
            case 3:
                result = CalculateEnum.DIVIDE.getValueZh();
                break;
            case 4:
                result = CalculateEnum.EQUAL.getValueZh();
                break;
            default:
                throw new RuntimeException("Invalid calculation method");
        }
        return result;
    }
}
