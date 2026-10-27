package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
@Entity
@Table(name = "metaforge_configure", uniqueConstraints = {
        @UniqueConstraint(name = "uk_configure_category_name", columnNames = {"category", "name"})
})
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class ConfigureEntity
        extends BaseEntity
{
    @Column(name = "category", length = 32, nullable = false)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String category;

    @Column(name = "configure", columnDefinition = "TEXT")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String configure;

    @Column(name = "description", length = 512)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;
}
