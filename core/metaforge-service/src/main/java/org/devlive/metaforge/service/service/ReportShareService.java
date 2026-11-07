package org.devlive.metaforge.service.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.common.response.CommonResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Public links and iframe embedding of reports. The share token is the only public identifier;
 * public reads run the report's saved definition as its owner and never accept SQL or a query
 * from the visitor.
 */
public interface ReportShareService
{
    /** Owner only: turn sharing on or off, allow embedding, restrict framing origins, rotate the link. */
    CommonResponse<ShareState> share(String code, ShareRequest request);

    /** The public view of a shared report, without its SQL, data source or owner. */
    CommonResponse<PublicReport> view(String token);

    /** Fresh rows of a shared dynamic query chart. */
    CommonResponse<List<Map<String, Object>>> refresh(String token);

    /** Rows of a shared dataset report, computed from the saved dataset query. */
    CommonResponse<Object> data(String token);

    /**
     * The CSP frame-ancestors value for an embeddable shared report, or empty when the report
     * cannot be embedded.
     */
    Optional<String> frameAncestors(String token);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ShareRequest
    {
        private boolean enabled;
        private boolean embed;
        private String origins;
        private boolean rotate;
        /** Epoch milliseconds after which the link stops working; null never expires. */
        private Long expiresAt;
        /** Show the report name and the description on the public page; null keeps them shown. */
        private Boolean showTitle;
        private Boolean showDescription;
        /** Offer the data as a table, and as a CSV download; null keeps them off. */
        private Boolean allowData;
        private Boolean allowDownload;

        public ShareRequest(boolean enabled, boolean embed, String origins, boolean rotate)
        {
            this(enabled, embed, origins, rotate, null, null, null, null, null);
        }

        public ShareRequest(boolean enabled, boolean embed, String origins, boolean rotate, Long expiresAt, Boolean showTitle, Boolean showDescription)
        {
            this(enabled, embed, origins, rotate, expiresAt, showTitle, showDescription, null, null);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ShareState
    {
        private boolean enabled;
        private boolean embed;
        private String origins;
        private String token;
        private Long expiresAt;
        private boolean showTitle;
        private boolean showDescription;
        private boolean allowData;
        private boolean allowDownload;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class PublicReport
    {
        private String name;
        private String description;
        private String type;
        private boolean realtime;
        private String configure;
        private boolean embed;
        private boolean showTitle;
        private boolean showDescription;
        private boolean allowData;
        private boolean allowDownload;
    }

    /** The report data as a table: column names and rows in the same order. */
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Plain response object, built once per request")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class Table
    {
        private List<String> headers;
        private List<List<Object>> rows;
        private boolean truncated;
    }

    /** The data table, when the owner offers it on the public page. */
    CommonResponse<Table> table(String token);

    /** The report name and data for a CSV download, when the owner allows downloads. */
    Optional<Map.Entry<String, Table>> download(String token);
}
