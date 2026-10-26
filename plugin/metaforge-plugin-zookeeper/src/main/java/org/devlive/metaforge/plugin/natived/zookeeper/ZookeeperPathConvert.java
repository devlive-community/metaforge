package org.devlive.metaforge.plugin.natived.zookeeper;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZookeeperPathConvert
{
    public static final String start = "/";
    public static final String regex = "`([A-Z|a-z|0-9|_|.|-]*)`";

    private ZookeeperPathConvert()
    {}

    public static String toPath(String source)
    {
        if (source.equalsIgnoreCase("all")) {
            return start;
        }
        // Replace `` tag content
        Pattern patten = Pattern.compile(regex);
        Matcher matcher = patten.matcher(source);
        List<String> matchers = new ArrayList<>();
        StringBuffer buffer = new StringBuffer();
        boolean appendStart = true;
        String withStart = null;
        while (matcher.find()) {
            if (matcher.start() == 0) {
                // "`sd.dd`.a.n.`d_dd`"
                buffer.append(start);
                buffer.append(matcher.group());
                withStart = matcher.group();
            }
            else if (matcher.start() > 0) {
                buffer.append(start);
                if (appendStart) {
                    if (StringUtils.isNotEmpty(withStart)) {
                        buffer.append(source.replace(withStart, "").substring(0, matcher.start() - withStart.length()).replace(".", start));
                    }
                    else {
                        buffer.append(source.substring(0, matcher.start()).replace(".", start));
                    }
                    buffer.append(matcher.group());
                    appendStart = false;
                }
                else {
                    buffer.append(matcher.group());
                }
            }
            matchers.add(matcher.group());
        }
        if (matchers.size() > 0) {
            return buffer.toString().replace("`", "").replace("//", start);
        }
        else {
            return start + source.replace(".", start);
        }
    }
}
