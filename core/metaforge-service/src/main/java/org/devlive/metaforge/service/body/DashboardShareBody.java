package org.devlive.metaforge.service.body;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * How a data screen is shared: with whom and how much (members and everyone), what viewers see
 * (all widgets or a selection), and the public link. Sent and returned whole by the owner.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class DashboardShareBody
{
    /** VIEW, EDIT or PUBLISH for every signed-in user; null keeps the screen to its members. */
    private String everyone;
    private List<Member> members = new ArrayList<>();
    /** Widget ids viewers see; null shows the whole screen. Editors always see everything. */
    private List<String> widgets;
    private PublicLink publicLink = new PublicLink();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Member
    {
        /** USER or ROLE */
        private String type;
        private Long id;
        /** Display name, filled in responses. */
        private String name;
        private String permission;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PublicLink
    {
        private boolean enabled;
        private boolean embed;
        /** Sites allowed to embed the screen, one origin per line; empty allows any site. */
        private String origins = "";
        /** Expiry as epoch milliseconds; null never expires. */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        private Long expiresAt;
        /** Response only. */
        private String token;
        /** Request only: issue a new token, invalidating the previous link. */
        private boolean rotate;
    }
}
