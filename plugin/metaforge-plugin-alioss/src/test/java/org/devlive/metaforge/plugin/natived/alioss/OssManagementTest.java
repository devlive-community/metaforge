package org.devlive.metaforge.plugin.natived.alioss;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectMetadata;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementDownload;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class OssManagementTest
{
    private final RuntimeConfigure configure = new RuntimeConfigure();
    private final ObjectMetadata metadata = new ObjectMetadata();
    private final ListObjectsV2Result listing = new ListObjectsV2Result();
    private ListObjectsV2Request listRequest;
    private GetObjectRequest readRequest;
    private GeneratePresignedUrlRequest signedRequest;
    private int opens;
    private int reads;
    private int closes;
    private byte[] content = new byte[0];
    private final OSS client = (OSS) Proxy.newProxyInstance(OSS.class.getClassLoader(), new Class<?>[] {OSS.class}, (proxy, method, arguments) -> {
        switch (method.getName()) {
            case "listObjectsV2":
                listRequest = (ListObjectsV2Request) arguments[0];
                return listing;
            case "getObjectMetadata":
                return metadata;
            case "getObject":
                readRequest = (GetObjectRequest) arguments[0];
                reads++;
                OSSObject object = new OSSObject();
                object.setObjectContent(new ByteArrayInputStream(content));
                return object;
            case "generatePresignedUrl":
                signedRequest = (GeneratePresignedUrlRequest) arguments[0];
                return new URL("https://objects.example.test/file");
            case "shutdown":
                closes++;
                return null;
            default:
                throw new AssertionError("Unexpected SDK operation: " + method.getName());
        }
    });
    private final OssManagement management = new OssManagement()
    {
        @Override
        protected OSS openClient(RuntimeConfigure ignored)
        {
            opens++;
            return client;
        }
    };

    public OssManagementTest()
    {
        configure.setDatabase(Optional.of("bucket-a"));
        metadata.setContentType("text/plain");
    }

    private ManagementRequest request()
    {
        ManagementRequest request = new ManagementRequest();
        request.setNamespace("bucket-a");
        request.setKey("docs/file.txt");
        return request;
    }

    @Test
    public void configuredBucketsDoNotRequireGlobalBucketListingPermission()
    {
        Assert.assertEquals("bucket-a", management.namespaces(configure, request()).getEntries().get(0).getId());
        Assert.assertEquals(0, opens);
    }

    @Test
    public void directoryPaginationUsesDelimiterAndOpaqueSdkTokens()
    {
        listing.addCommonPrefix("docs/sub/");
        OSSObjectSummary object = new OSSObjectSummary();
        object.setKey("docs/file.txt");
        object.setSize(12);
        listing.addObjectSummary(object);
        listing.setTruncated(true);
        listing.setNextContinuationToken("next+/=");
        ManagementRequest request = request();
        request.setPrefix("docs/");
        request.setCursor("previous+/=");
        request.setLimit(50);
        ManagementPage result = management.list(configure, request);
        Assert.assertEquals("docs/", listRequest.getPrefix());
        Assert.assertEquals("/", listRequest.getDelimiter());
        Assert.assertEquals("previous+/=", listRequest.getContinuationToken());
        Assert.assertEquals(List.of("folder", "object"), List.of(result.getEntries().get(0).getType(), result.getEntries().get(1).getType()));
        Assert.assertEquals("next+/=", result.getNextCursor());
        Assert.assertTrue(result.isHasMore());
        Assert.assertEquals(opens, closes);
    }

    @Test
    public void contentIsReadOnlyOnDemandAndLimitedTo64KiB()
    {
        content = "x".repeat(100000).getBytes(StandardCharsets.UTF_8);
        metadata.setContentLength(content.length);
        ManagementRequest request = request();
        Assert.assertNull(management.detail(configure, request).getContent());
        Assert.assertEquals(0, reads);
        request.setPreview(true);
        ManagementDetail detail = management.detail(configure, request);
        Assert.assertEquals(65536, detail.getContent().length());
        Assert.assertTrue(detail.isTruncated());
        Assert.assertArrayEquals(new long[] {0, 65535}, readRequest.getRange());
        Assert.assertEquals(opens, closes);
    }

    @Test
    public void binaryPreviewsRemainBase64AndDownloadsUseShortLivedGetLinks()
    {
        content = new byte[] {0, (byte) 255};
        metadata.setContentLength(content.length);
        ManagementRequest request = request();
        request.setPreview(true);
        ManagementDetail detail = management.detail(configure, request);
        Assert.assertEquals("base64", detail.getEncoding());
        Assert.assertArrayEquals(content, Base64.getDecoder().decode(detail.getContent()));
        request.setKey("docs/资料 & file.txt");
        ManagementDownload download = management.download(configure, request);
        Assert.assertEquals(HttpMethod.GET, signedRequest.getMethod());
        Assert.assertTrue(download.getExpiresAt() - System.currentTimeMillis() <= 300000);
        Assert.assertTrue(download.getExpiresAt() - System.currentTimeMillis() > 290000);
        Assert.assertEquals("资料 & file.txt", download.getFileName());
        Assert.assertTrue(signedRequest.getResponseHeaders().getContentDisposition().contains("%20%26%20"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aRequestCannotBrowseOutsideTheConfiguredBucket()
    {
        ManagementRequest request = request();
        request.setNamespace("other-bucket");
        try {
            management.list(configure, request);
        }
        finally {
            Assert.assertEquals(0, opens);
        }
    }
}
