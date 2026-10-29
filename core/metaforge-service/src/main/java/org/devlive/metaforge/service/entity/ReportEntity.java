package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.enums.ReportType;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("metaforge_report")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class ReportEntity
        extends BaseEntity
{
    @Column("configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String configure;

    @Column("realtime")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean realtime;

    @Column("type")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private ReportType type;

    @Column("query")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String query;

    @Column("description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column("user_id")
    @JsonView(value = {EntityView.AdminView.class})
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonView(value = {EntityView.AdminView.class})
    private UserEntity user;

    @Column("source_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long sourceId;

    @Transient
    @Reference("sourceId")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity source;

    @Column("dataset_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long datasetId;

    @Transient
    @Reference("datasetId")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private DataSetEntity dataset;
}
