package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.settings.AuthSettings;
import org.devlive.metaforge.service.settings.MailSettings;

import java.util.Map;

/** Settings an administrator changes in system settings: sign-in and registration features and the mail server. */
public interface SystemSettingService
{
    AuthSettings auth();

    MailSettings mail();

    /** Both groups for the settings page, the mail password left out. */
    Map<String, Object> settings();

    /** Saves both groups; an empty mail password keeps the stored one. */
    CommonResponse<Map<String, Object>> save(AuthSettings auth, MailSettings mail);

    /** Sends a test mail with the saved server, or with the given one when it is passed. */
    CommonResponse<Boolean> testMail(MailSettings mail, String to);
}
