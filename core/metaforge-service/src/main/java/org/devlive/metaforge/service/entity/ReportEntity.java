package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
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

import java.util.Date;

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

    // Public link and iframe embedding. The token is the only public identifier of a shared report;
    // only its owner reads these fields and they change through the share endpoint, never a save.
    @Column("share_token")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String shareToken;

    @Column("share_enabled")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean shareEnabled;

    @Column("share_embed")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean shareEmbed;

    // Origins allowed to frame the report, one per line; empty allows any site.
    @Column("share_origins")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String shareOrigins;

    // The link stops working after this instant; null never expires. Sent as epoch milliseconds.
    @Column("share_expires_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Date shareExpiresAt;

    @Column("share_show_title")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private boolean shareShowTitle = true;

    @Column("share_show_description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private boolean shareShowDescription = true;

    @Transient
    @Reference("datasetId")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private DataSetEntity dataset;
}
