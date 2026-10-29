package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 通用配置表，承载 EXECUTOR / DATASET 等不同范畴的运行时配置。
 * 字段 schema 由各插件通过 Plugin.runtimeConfigures() 声明；value 为序列化后的 JSON。
 *
 * Generic configuration row keyed by (category, name).
 * - category: e.g. "EXECUTOR", "DATASET"
 * - name:     e.g. "Local", "Seatunnel", or a dataset target identifier
 * - configure: JSON-serialized map of effective field values
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("metaforge_configure")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class ConfigureEntity
        extends BaseEntity
{
    @Column("category")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String category;

    @Column("configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String configure;

    @Column("description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;
}
