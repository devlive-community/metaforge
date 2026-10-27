package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureExecutor;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "metaforge_source")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", "EQ_DOESNT_OVERRIDE_EQUALS"},
        justification = "I prefer to suppress these FindBugs warnings")
public class SourceEntity
        extends BaseEntity
{
    @Column(name = "description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column(name = "_type", nullable = false)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String type;

    @Column(name = "protocol", unique = true, nullable = false, columnDefinition = "varchar default 'HTTP'")
    @NotNull(message = "The passed protocol cannot by empty")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String protocol;

    @Column(name = "host", unique = true, nullable = false)
    @NotEmpty(message = "The passed host cannot by empty")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String host;

    @Column(name = "port", unique = true, nullable = false)
    @NotNull(message = "The passed port cannot by empty")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Integer port = 0;

    @Column(name = "username")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String username;

    @Column(name = "password")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String password;

    @Column(name = "_catalog")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String catalog;

    @Column(name = "_database")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String database;

    // Add from 1.1.0.20221115
    @Column(name = "_ssl", columnDefinition = "boolean default false")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean ssl;

    @OneToMany(mappedBy = "source", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PluginAuditEntity> pluginAudits;

    // Add from 1.1.0.20221115
    @Column(name = "public", columnDefinition = "boolean default false")
    @JsonProperty(value = "public")
    private Boolean publish; // Public use or not

    @Column(name = "configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String originalConfigure;

    @Column(name = "used_config")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean usedConfig;

    @Column(name = "version")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String version;

    @Column(name = "available")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean available;

    @Column(name = "message")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @Transient
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Map<String, Object> configures;

    @Transient
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private PluginConfigure schema;

    @Transient
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<PluginConfigureExecutor> pipelines;

    @Transient
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String home;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIncludeProperties(value = {"id", "username", "code"})
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;

    @OneToMany(mappedBy = "source", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<ScheduledHistoryEntity> historys;

    @Transient
    private String name;

    @Transient
    @JsonProperty(value = "configure")
    private PluginConfigure configure;

    @Transient
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean isSupportMeta;

    public void setOriginalConfigure(String configure)
    {
        this.originalConfigure = configure;
        if (StringUtils.isNotEmpty(configure)) {
            this.setConfigures(JsonUtils.toMap(this.originalConfigure));
        }
    }

    public Map<String, Object> getConfigures()
    {
        if (StringUtils.isNotEmpty(this.originalConfigure)) {
            this.setConfigures(JsonUtils.toMap(this.originalConfigure));
        }
        return configures;
    }

    public RuntimeConfigure toConfigure(PluginManager pluginManager, Plugin plugin)
    {
        return toConfigure("JsonConvert", pluginManager, plugin);
    }

    /**
     * Converts the current object to a RuntimeConfigure object.
     *
     * @return The RuntimeConfigure object created from the current object.
     */
    public RuntimeConfigure toConfigure(String format, PluginManager pluginManager, Plugin plugin)
    {
        return RuntimeConfigure.builder()
                .host(this.getHost())
                .port(this.getPort())
                .username(Optional.ofNullable(this.getUsername()))
                .password(Optional.ofNullable(this.getPassword()))
                .database(StringUtils.isNotEmpty(this.getDatabase()) ? Optional.ofNullable(this.getDatabase()) : Optional.empty())
                .ssl(Optional.ofNullable(this.getSsl()))
                .env(Optional.ofNullable(this.getConfigures()))
                .format(format)
                .usedConfig(this.isUsedConfig())
                .pluginManager(pluginManager)
                .plugin(plugin)
                .id(String.valueOf(this.getId()))
                .home(this.getHome())
                .build();
    }
}
