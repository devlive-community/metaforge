package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.itransient.configuration.Configuration;

public interface ConfigureService
{
    CommonResponse<Configuration> getExecutor();
}
