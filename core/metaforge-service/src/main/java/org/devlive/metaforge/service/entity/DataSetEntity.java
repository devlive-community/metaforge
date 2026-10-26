package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.enums.DataSetState;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.converter.ListConverter;
import org.devlive.metaforge.service.enums.SyncMode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.util.List;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metaforge_dataset")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class DataSetEntity
        extends BaseEntity
{
    @Column(name = "description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column(name = "query")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String query;

    @Column(name = "sync_mode")
    @Enumerated(EnumType.STRING)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SyncMode syncMode = SyncMode.MANUAL;

    @Column(name = "expression")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String expression; // only for TIMING

    @Column(name = "state")
    @Convert(converter = ListConverter.class)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<DataSetState> state;

    @Column(name = "message")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @Column(name = "table_name")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String tableName;

    @Column(name = "scheduler")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String scheduler;

    @Column(name = "executor")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String executor;

    @Column(name = "total_rows")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String totalRows;

    @Column(name = "total_size")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String totalSize;

    @Column(name = "life_cycle")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String lifeCycle;

    @Column(name = "life_cycle_column")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String lifeCycleColumn;

    @Column(name = "life_cycle_type")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String lifeCycleType;

    @ManyToOne
    @JoinTable(name = "metaforge_dataset_source_relation",
            joinColumns = @JoinColumn(name = "dataset_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id"))
    @JsonIgnoreProperties(value = {"user"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity source;

    @ManyToOne
    @JoinTable(name = "metaforge_dataset_user_relation",
            joinColumns = @JoinColumn(name = "dataset_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnoreProperties(value = {"roles", "thirdConfigure", "avatarConfigure"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;

    @Transient
    private Set<DataSetColumnEntity> columns;
}
