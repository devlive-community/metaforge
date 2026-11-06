package org.devlive.metaforge.service.repository.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devlive.metaforge.service.enums.SharePermission;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sharing of data screens, read and replaced with explicit SQL. Callers run replacements inside
 * their own transaction.
 */
@Repository
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class DashboardShareRepository
{
    public static final String USER = "USER";
    public static final String ROLE = "ROLE";

    private final NamedParameterJdbcOperations jdbc;

    public DashboardShareRepository(NamedParameterJdbcOperations jdbc)
    {
        this.jdbc = jdbc;
    }

    @Data
    @NoArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static class Share
    {
        private Long dashboardId;
        private String everyonePermission;
        private String widgets;
        private boolean publicEnabled;
        private String publicToken;
        private boolean publicEmbed;
        private String publicOrigins;
        private Date publicExpiresAt;
    }

    @Data
    @NoArgsConstructor
    public static class Member
    {
        private String type;
        private Long id;
        private String permission;

        public Member(String type, Long id, String permission)
        {
            this.type = type;
            this.id = id;
            this.permission = permission;
        }
    }

    public Optional<Share> find(Long dashboardId)
    {
        return jdbc.query("SELECT * FROM metaforge_dashboard_share WHERE dashboard_id = :id", Map.of("id", dashboardId), (rs, row) -> {
            Share share = new Share();
            share.setDashboardId(rs.getLong("dashboard_id"));
            share.setEveryonePermission(rs.getString("everyone_permission"));
            share.setWidgets(rs.getString("widgets"));
            share.setPublicEnabled(rs.getBoolean("public_enabled"));
            share.setPublicToken(rs.getString("public_token"));
            share.setPublicEmbed(rs.getBoolean("public_embed"));
            share.setPublicOrigins(rs.getString("public_origins"));
            Timestamp expires = rs.getTimestamp("public_expires_at");
            share.setPublicExpiresAt(expires == null ? null : new Date(expires.getTime()));
            return share;
        }).stream().findFirst();
    }

    /** The screen behind an enabled public link with this token. */
    public Optional<Long> findPublic(String token)
    {
        return jdbc.queryForList("SELECT dashboard_id FROM metaforge_dashboard_share WHERE public_token = :token AND public_enabled = :enabled",
                new MapSqlParameterSource("token", token).addValue("enabled", true), Long.class).stream().findFirst();
    }

    public void save(Share share)
    {
        jdbc.update("DELETE FROM metaforge_dashboard_share WHERE dashboard_id = :id", Map.of("id", share.getDashboardId()));
        jdbc.update("INSERT INTO metaforge_dashboard_share (dashboard_id, everyone_permission, widgets, public_enabled, public_token, public_embed, "
                        + "public_origins, public_expires_at, update_time) VALUES (:id, :everyone, :widgets, :enabled, :token, :embed, :origins, :expires, :now)",
                new MapSqlParameterSource("id", share.getDashboardId())
                        .addValue("everyone", share.getEveryonePermission())
                        .addValue("widgets", share.getWidgets())
                        .addValue("enabled", share.isPublicEnabled())
                        .addValue("token", share.getPublicToken())
                        .addValue("embed", share.isPublicEmbed())
                        .addValue("origins", share.getPublicOrigins())
                        .addValue("expires", share.getPublicExpiresAt() == null ? null : new Timestamp(share.getPublicExpiresAt().getTime()))
                        .addValue("now", new Timestamp(System.currentTimeMillis())));
    }

    public List<Member> members(Long dashboardId)
    {
        return jdbc.query("SELECT member_type, member_id, permission FROM metaforge_dashboard_member WHERE dashboard_id = :id ORDER BY member_type, member_id",
                Map.of("id", dashboardId), (rs, row) -> new Member(rs.getString("member_type"), rs.getLong("member_id"), rs.getString("permission")));
    }

    public void replaceMembers(Long dashboardId, Collection<Member> members)
    {
        jdbc.update("DELETE FROM metaforge_dashboard_member WHERE dashboard_id = :id", Map.of("id", dashboardId));
        for (Member member : members) {
            jdbc.update("INSERT INTO metaforge_dashboard_member (dashboard_id, member_type, member_id, permission) VALUES (:id, :type, :member, :permission)",
                    new MapSqlParameterSource("id", dashboardId).addValue("type", member.getType()).addValue("member", member.getId())
                            .addValue("permission", member.getPermission()));
        }
    }

    public void delete(Long dashboardId)
    {
        jdbc.update("DELETE FROM metaforge_dashboard_member WHERE dashboard_id = :id", Map.of("id", dashboardId));
        jdbc.update("DELETE FROM metaforge_dashboard_share WHERE dashboard_id = :id", Map.of("id", dashboardId));
    }

    /**
     * Screens shared with a user directly, through one of their roles or with everyone, each with
     * the highest permission any of these grants.
     */
    public Map<Long, SharePermission> sharedWith(Long userId, Collection<Long> roleIds)
    {
        Map<Long, SharePermission> result = new LinkedHashMap<>();
        MapSqlParameterSource params = new MapSqlParameterSource("user", userId).addValue("userType", USER).addValue("roleType", ROLE)
                .addValue("roles", roleIds.isEmpty() ? List.of(-1L) : new ArrayList<>(roleIds));
        jdbc.query("SELECT dashboard_id, permission FROM metaforge_dashboard_member WHERE (member_type = :userType AND member_id = :user) "
                + "OR (member_type = :roleType AND member_id IN (:roles))", params, rs -> {
                    merge(result, rs.getLong("dashboard_id"), rs.getString("permission"));
                });
        jdbc.query("SELECT dashboard_id, everyone_permission FROM metaforge_dashboard_share WHERE everyone_permission IS NOT NULL", Map.of(), rs -> {
            merge(result, rs.getLong("dashboard_id"), rs.getString("everyone_permission"));
        });
        return result;
    }

    private static void merge(Map<Long, SharePermission> result, Long dashboardId, String permission)
    {
        SharePermission.grantable(permission).ifPresent(granted -> result.merge(dashboardId, granted,
                (left, right) -> left.atLeast(right) ? left : right));
    }
}
