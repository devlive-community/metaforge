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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "metaforge_report")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class ReportEntity
        extends BaseEntity
{
    @Column(name = "configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String configure;

    @Column(name = "realtime")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean realtime;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private ReportType type;

    @Column(name = "query")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String query;

    @Column(name = "description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @ManyToOne
    @JoinTable(name = "metaforge_report_user_relation",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonView(value = {EntityView.AdminView.class})
    private UserEntity user;

    @ManyToOne
    @JoinTable(name = "metaforge_report_source_relation",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id"))
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity source;

    @ManyToOne
    @JoinTable(name = "metaforge_report_dataset_relation",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "dataset_id"))
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private DataSetEntity dataset;
}
