package org.devlive.metaforge.common.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@SuppressFBWarnings(value = {"ME_ENUM_FIELD_SETTER"},
        justification = "I prefer to suppress these FindBugs warnings")
public class AiSupportUtils
{
    private AiSupportUtils()
    {}

    public static String getValue(String locale, AiSupportEnum supportType)
    {
        if (StringUtils.isEmpty(locale)) {
            return supportType.enValue;
        }
        if (locale.equalsIgnoreCase("zh")) {
            return supportType.zhValue;
        }
        else {
            return supportType.enValue;
        }
    }

    @AllArgsConstructor
    public enum AiSupportEnum
    {
        OPTIMIZE("优化以下SQL\n${sql}\n输出为markdown\n引擎是${engine}", "Help me optimize the following SQL\n${sql}\noutput is markdown\nThe engine is ${engine}"),
        ANALYSIS("解析以下SQL\n${sql}\n输出为markdown\n引擎是${engine}", "Help me analyze the following SQL\n${sql}\noutput is markdown\nThe engine is ${engine}"),
        TRANSLATOR("将此SQL查询翻译成自然语言\n${sql}\n输出为markdown", "Translate this SQL query into natural language\n${sql}\noutput is markdown"),
        FINDBUGS("查找以下SQL出现的错误信息\n${sql}\n输出为markdown\n引擎是${engine}", "Find the error message that appears in the following SQL\n${sql}\nThe output is markdown\nThe engine is ${engine}"),
        FIXEDBUGS("修复一下SQL出现的错误\n${sql}\n输出返回结果内容为markdown\n引擎是${engine}\n出现的错误是\n${error}", "Fix the error in SQL\n${sql}\nThe output returns the result content as markdown\nThe engine is ${engine}\nThe error that occurred is\n${error}");

        @Getter
        @Setter
        private String zhValue;
        @Getter
        @Setter
        private String enValue;
    }
}
