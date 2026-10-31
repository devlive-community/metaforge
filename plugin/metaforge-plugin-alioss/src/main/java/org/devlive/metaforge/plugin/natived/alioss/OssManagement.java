package org.devlive.metaforge.plugin.natived.alioss;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.BucketList;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ListBucketsRequest;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementDownload;
import org.devlive.metaforge.spi.model.ManagementEntry;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Native OSS directory browsing and bounded previews. A configured bucket is a scope boundary. */
public class OssManagement
{
    private static final int PREVIEW_BYTES = 65536;

    public ManagementPage namespaces(RuntimeConfigure configure, ManagementRequest request)
    {
        if (configure.getDatabase().filter(value -> !value.isBlank()).isPresent()) {
            ManagementEntry entry = new ManagementEntry();
            entry.setId(configure.getDatabase().get());
            entry.setName(entry.getId());
            entry.setType("bucket");
            ManagementPage page = new ManagementPage();
            if (request.getPrefix().isEmpty() || entry.getName().startsWith(request.getPrefix())) {
                page.getEntries().add(entry);
            }
            page.setNamespace(entry.getId());
            return page;
        }
        return withClient(configure, client -> {
            ListBucketsRequest query = new ListBucketsRequest();
            query.setPrefix(request.getPrefix());
            query.setMarker(request.getCursor().isEmpty() ? null : request.getCursor());
            query.setMaxKeys(request.getLimit());
            BucketList buckets = client.listBuckets(query);
            ManagementPage page = new ManagementPage();
            buckets.getBucketList().forEach(bucket -> {
                ManagementEntry entry = new ManagementEntry();
                entry.setId(bucket.getName());
                entry.setName(bucket.getName());
                entry.setType("bucket");
                if (bucket.getCreationDate() != null) {
                    entry.setModifiedAt(bucket.getCreationDate().getTime());
                }
                page.getEntries().add(entry);
            });
            page.setHasMore(buckets.isTruncated());
            page.setNextCursor(buckets.getNextMarker());
            return page;
        });
    }

    public ManagementPage list(RuntimeConfigure configure, ManagementRequest request)
    {
        String bucket = namespace(configure, request);
        return withClient(configure, client -> {
            ListObjectsV2Request query = new ListObjectsV2Request(bucket);
            query.setPrefix(request.getPrefix());
            query.setDelimiter("/");
            query.setMaxKeys(request.getLimit());
            query.setContinuationToken(request.getCursor().isEmpty() ? null : request.getCursor());
            ListObjectsV2Result result = client.listObjectsV2(query);
            Map<String, ManagementEntry> entries = new LinkedHashMap<>();
            for (String prefix : result.getCommonPrefixes()) {
                ManagementEntry entry = new ManagementEntry();
                entry.setId(prefix);
                entry.setName(relativeName(prefix, request.getPrefix()));
                entry.setType("folder");
                entries.put(prefix, entry);
            }
            for (OSSObjectSummary object : result.getObjectSummaries()) {
                if (object.getKey().equals(request.getPrefix())) {
                    continue;
                }
                ManagementEntry entry = new ManagementEntry();
                entry.setId(object.getKey());
                entry.setName(relativeName(object.getKey(), request.getPrefix()));
                entry.setType(object.getKey().endsWith("/") ? "folder" : "object");
                entry.setSize(object.getSize());
                if (object.getLastModified() != null) {
                    entry.setModifiedAt(object.getLastModified().getTime());
                }
                if (object.getStorageClass() != null) {
                    entry.getAttributes().put("storageClass", object.getStorageClass());
                }
                entries.putIfAbsent(entry.getId(), entry);
            }
            ManagementPage page = new ManagementPage();
            page.getEntries().addAll(entries.values());
            page.setNamespace(bucket);
            page.setPrefix(request.getPrefix());
            page.setHasMore(result.isTruncated());
            page.setNextCursor(result.getNextContinuationToken());
            return page;
        });
    }

    public ManagementDetail detail(RuntimeConfigure configure, ManagementRequest request)
    {
        String bucket = namespace(configure, request);
        String key = objectKey(request);
        return withClient(configure, client -> {
            ObjectMetadata metadata = client.getObjectMetadata(bucket, key);
            ManagementEntry entry = new ManagementEntry();
            entry.setId(key);
            entry.setName(key);
            entry.setType("object");
            entry.setSize(metadata.getContentLength());
            if (metadata.getLastModified() != null) {
                entry.setModifiedAt(metadata.getLastModified().getTime());
            }
            if (metadata.getContentType() != null) {
                entry.getAttributes().put("contentType", metadata.getContentType());
            }
            if (metadata.getETag() != null) {
                entry.getAttributes().put("etag", metadata.getETag());
            }
            metadata.getUserMetadata().forEach((name, value) -> entry.getAttributes().put("metadata:" + name, value));
            ManagementDetail detail = new ManagementDetail();
            detail.setEntry(entry);
            if (request.isPreview()) {
                byte[] content = new byte[0];
                if (metadata.getContentLength() > 0) {
                    GetObjectRequest get = new GetObjectRequest(bucket, key);
                    get.setRange(0, Math.min(metadata.getContentLength(), PREVIEW_BYTES) - 1);
                    try (OSSObject object = client.getObject(get); InputStream input = object.getObjectContent()) {
                        content = input.readNBytes(PREVIEW_BYTES);
                    }
                    catch (IOException error) {
                        throw new IllegalStateException("Object preview could not be read", error);
                    }
                }
                String text = decodeText(content);
                detail.setEncoding(text == null ? "base64" : "utf-8");
                detail.setContent(text == null ? Base64.getEncoder().encodeToString(content) : text);
                detail.setTruncated(metadata.getContentLength() > content.length);
            }
            return detail;
        });
    }

    public ManagementDownload download(RuntimeConfigure configure, ManagementRequest request)
    {
        String bucket = namespace(configure, request);
        String key = objectKey(request);
        return withClient(configure, client -> {
            Date expires = new Date(System.currentTimeMillis() + 300000);
            GeneratePresignedUrlRequest get = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            get.setExpiration(expires);
            String name = key.substring(key.lastIndexOf('/') + 1);
            ResponseHeaderOverrides headers = new ResponseHeaderOverrides();
            headers.setContentDisposition("attachment; filename*=UTF-8''" + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20"));
            get.setResponseHeaders(headers);
            ManagementDownload download = new ManagementDownload();
            download.setUrl(client.generatePresignedUrl(get).toExternalForm());
            download.setFileName(name);
            download.setExpiresAt(expires.getTime());
            return download;
        });
    }

    protected OSS openClient(RuntimeConfigure configure)
    {
        String endpoint = configure.getHost();
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("OSS endpoint is required");
        }
        if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
            endpoint = (configure.getSsl().orElse(true) ? "https://" : "http://") + endpoint;
        }
        return new OSSClientBuilder().build(endpoint,
                configure.getUsername().orElseThrow(() -> new IllegalArgumentException("OSS access key is required")),
                configure.getPassword().orElseThrow(() -> new IllegalArgumentException("OSS access secret is required")));
    }

    private <T> T withClient(RuntimeConfigure configure, Function<OSS, T> action)
    {
        OSS client = openClient(configure);
        try {
            return action.apply(client);
        }
        finally {
            client.shutdown();
        }
    }

    private String namespace(RuntimeConfigure configure, ManagementRequest request)
    {
        String configured = configure.getDatabase().filter(value -> !value.isBlank()).orElse(null);
        String selected = request.getNamespace() == null || request.getNamespace().isBlank() ? configured : request.getNamespace();
        if (selected == null || (configured != null && !configured.equals(selected))) {
            throw new IllegalArgumentException("Select a bucket allowed by this source connection");
        }
        return selected;
    }

    private String objectKey(ManagementRequest request)
    {
        if (request.getKey() == null || request.getKey().isEmpty() || request.getKey().endsWith("/")) {
            throw new IllegalArgumentException("Select a file object");
        }
        return request.getKey();
    }

    private String relativeName(String key, String prefix)
    {
        return key.startsWith(prefix) ? key.substring(prefix.length()) : key;
    }

    private String decodeText(byte[] content)
    {
        try {
            String value = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
            return value.codePoints().anyMatch(character -> Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t') ? null : value;
        }
        catch (CharacterCodingException error) {
            return null;
        }
    }
}
