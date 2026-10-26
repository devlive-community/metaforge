package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;

public interface FormatService
{
    CommonResponse<String> formatterSql(String sql);
}
