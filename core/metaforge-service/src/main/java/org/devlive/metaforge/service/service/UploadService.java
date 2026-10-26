package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.UploadBody;

public interface UploadService
{
    CommonResponse upload(UploadBody configure);
}
