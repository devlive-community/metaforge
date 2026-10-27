package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.activity.HeatmapActivity;
import org.devlive.metaforge.service.entity.PluginAuditEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface PluginAuditRepository
        extends BaseRepository<PluginAuditEntity, Long>
{
    @Override
    default Specification<PluginAuditEntity> visibleTo(UserEntity user)
    {
        return (root, query, builder) -> builder.equal(root.get("user"), user);
    }

    Page<PluginAuditEntity> findAllByUser(UserEntity user, Pageable pageable);

    Long countByUser(UserEntity user);

    @Query(value = "SELECT new org.devlive.metaforge.service.activity.HeatmapActivity(COUNT(1), CAST(p.createTime AS date)) " +
            "FROM PluginAuditEntity AS p " +
            "WHERE p.user = :user " +
            "GROUP BY CAST(p.createTime AS date) " +
            "ORDER BY CAST(p.createTime AS date) ASC")
    List<HeatmapActivity> countByCreateTimeAndFindByUser(@Param(value = "user") UserEntity user);

    @Query(value = "SELECT count(1) AS dataOfCount, s.name AS dataOfLabel\n" +
            "FROM metaforge_source_query AS ap\n" +
            "LEFT JOIN metaforge_source AS s ON ap.plugin_id = s.id\n" +
            "WHERE date_sub(curdate(), interval 7 day) <= date(ap.create_time) AND s.user_id = ?1\n" +
            "GROUP BY s.name", nativeQuery = true)
    List<Map<String, Object>> selectRadarByUserId(Long id);
}
