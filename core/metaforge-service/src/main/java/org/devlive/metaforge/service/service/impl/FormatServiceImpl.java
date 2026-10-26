package org.devlive.metaforge.service.service.impl;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.FormatService;
import org.springframework.stereotype.Service;

@Service
public class FormatServiceImpl
        implements FormatService
{
    @Override
    public CommonResponse<String> formatterSql(String sql)
    {
        String formatterSql = SqlFormatter.of(Dialect.MySql)
                .format(sql);
        return CommonResponse.success(formatterSql);
    }
}
