package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.DashboardShareBody;
import org.devlive.metaforge.service.entity.DashboardEntity;

import java.util.List;
import java.util.Map;

public interface DashboardService
        extends BaseService<DashboardEntity>
{
    /**
     * Opens one of the current user's published data screens as their home page, or restores the
     * default home page when {@code code} is null.
     */
    CommonResponse<String> setHomePage(String code);

    /** Screens other users share with the signed-in user, each with the user's permission. */
    CommonResponse<List<DashboardEntity>> shared();

    CommonResponse<List<DashboardEntity>> shared(String keyword, String sort);

    /** Refreshes a dynamic chart of a screen for anyone who may see it; reports run as the owner. */
    CommonResponse<List<Map<String, Object>>> refreshChart(String code, String reportCode);

    /** How the owner shares a screen. */
    CommonResponse<DashboardShareBody> getShare(String code);

    /** Replaces how the owner shares a screen. */
    CommonResponse<DashboardShareBody> saveShare(String code, DashboardShareBody body);

    /** A published screen behind an enabled, unexpired public link, as anonymous visitors see it. */
    CommonResponse<Map<String, Object>> publicView(String token);

    /** Refreshes a dynamic chart of a public screen; it runs as the owner. */
    CommonResponse<List<Map<String, Object>>> publicRefresh(String token, String reportCode);

    /** The frame-ancestors sources of an embeddable public screen; empty when it may not be framed. */
    java.util.Optional<String> publicFrameAncestors(String token);

    /** Users and roles the owner can share with, matching a keyword. */
    CommonResponse<List<DashboardShareBody.Member>> shareCandidates(String keyword);
}
