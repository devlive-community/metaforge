package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.DataSetState;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.enums.SyncMode;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_dataset")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class DataSetEntity
        extends BaseEntity
{
    @Column("description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column("query")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String query;

    @Column("sync_mode")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SyncMode syncMode = SyncMode.MANUAL;

    @Column("expression")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String expression; // only for TIMING

    // Comma separated DataSetState names; exposed as a list through getState/setState.
    @Column("state")
    @JsonIgnore
    private String stateValue;

    @Column("message")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @Column("table_name")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String tableName;

    @Column("scheduler")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String scheduler;

    @Column("executor")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String executor;

    @Column("total_rows")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long totalRows;

    @Column("total_size")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String totalSize;

    @Column("life_cycle")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long lifeCycle;

    @Column("life_cycle_column")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String lifeCycleColumn;

    @Column("life_cycle_type")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String lifeCycleType;

    @Column("source_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long sourceId;

    @Transient
    @Reference("sourceId")
    @JsonIgnoreProperties(value = {"user"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private SourceEntity source;

    @Column("user_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonIgnoreProperties(value = {"roles", "thirdConfigure", "avatarConfigure", "sources"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;

    @Transient
    private Set<DataSetColumnEntity> columns;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    public List<DataSetState> getState()
    {
        List<DataSetState> states = new ArrayList<>();
        if (StringUtils.isNotEmpty(stateValue)) {
            for (String value : stateValue.split(",")) {
                states.add(DataSetState.valueOf(value.trim()));
            }
        }
        return states;
    }

    public void setState(List<DataSetState> state)
    {
        this.stateValue = state == null || state.isEmpty() ? null
                : state.stream().filter(Objects::nonNull).map(Enum::name).collect(Collectors.joining(","));
    }
}
