package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.settings.AuthSettings;
import org.devlive.metaforge.service.settings.FsSettings;
import org.devlive.metaforge.service.settings.MailSettings;
import org.devlive.metaforge.service.settings.OAuthProvider;
import org.devlive.metaforge.service.settings.OAuthSettings;

import java.util.Map;

/** Settings an administrator changes in system settings: sign-in and registration features, the mail server and third-party sign-in. */
public interface SystemSettingService
{
    AuthSettings auth();

    MailSettings mail();

    OAuthSettings oauth(OAuthProvider provider);

    /** Where uploads and query results are stored. */
    FsSettings fs();

    /** Every group for the settings page, the mail password and client secrets left out. */
    Map<String, Object> settings();

    /**
     * Saves the groups; an empty mail password or client secret keeps the stored one.
     *
     * @param oauth the providers by id (github, google, oidc); providers left out, or all of them when null, stay as they are
     */
    default CommonResponse<Map<String, Object>> save(AuthSettings auth, MailSettings mail, Map<String, OAuthSettings> oauth)
    {
        return save(auth, mail, oauth, null);
    }

    /**
     * Saves the groups as {@link #save(AuthSettings, MailSettings, Map)} does, and file storage when
     * it is passed; an empty secret keeps the stored one.
     */
    CommonResponse<Map<String, Object>> save(AuthSettings auth, MailSettings mail, Map<String, OAuthSettings> oauth, FsSettings fs);

    /** Writes and deletes a probe file with the given storage, an empty secret meaning the saved one. */
    CommonResponse<Boolean> testFs(FsSettings fs);

    /**
     * Tries runtime settings before they are saved: an executor plugin checks its values, and the
     * dataset target connects with them.
     *
     * @param category EXECUTOR or DATASET
     * @param name the executor plugin's name, or the dataset target's
     */
    CommonResponse<String> testRuntime(String category, String name, Map<String, String> values);

    /** Sends a test mail with the saved server, or with the given one when it is passed. */
    CommonResponse<Boolean> testMail(MailSettings mail, String to);
}
