package org.devlive.metaforge.service.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where uploads and query results are stored: one of the installed file system plugins (LocalFs,
 * AliOssFs, AmazonS3Fs, MinIOFs, QiniuFs, TencentCosFs) and the account, endpoint and bucket of the
 * object stores.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FsSettings
{
    public static final String LOCAL = "LocalFs";

    private String type;
    private String access;
    private String secret;
    private String endpoint;
    private String bucket;
    /** Read only: whether a secret is stored, since the secret itself is never sent back. */
    private boolean secretSet;

    public static FsSettings from(Map<String, String> values)
    {
        String secret = SystemSettingSchema.text(values.get("secret"));
        String type = SystemSettingSchema.text(values.get("type"));
        return FsSettings.builder()
                .type(type.isEmpty() ? LOCAL : type)
                .access(SystemSettingSchema.text(values.get("access")))
                .secret(secret)
                .endpoint(SystemSettingSchema.text(values.get("endpoint")))
                .bucket(SystemSettingSchema.text(values.get("bucket")))
                .secretSet(!secret.isEmpty())
                .build();
    }

    public Map<String, String> toMap()
    {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("type", StringUtils.isBlank(type) ? LOCAL : type.trim());
        values.put("access", SystemSettingSchema.text(access));
        values.put("secret", SystemSettingSchema.text(secret));
        values.put("endpoint", SystemSettingSchema.text(endpoint));
        values.put("bucket", SystemSettingSchema.text(bucket));
        return values;
    }

    /** The local file system needs no account; it writes under the data directory. */
    public boolean local()
    {
        return StringUtils.isBlank(type) || type.startsWith("Local");
    }

    /** An object store needs its account, endpoint and bucket. */
    public boolean configured()
    {
        return local() || (StringUtils.isNotBlank(access) && StringUtils.isNotBlank(secret) && StringUtils.isNotBlank(endpoint) && StringUtils.isNotBlank(bucket));
    }

    /** The settings as the administrator sees them: everything except the secret. */
    public FsSettings masked()
    {
        return toBuilder().secret("").secretSet(StringUtils.isNotEmpty(secret)).build();
    }
}
