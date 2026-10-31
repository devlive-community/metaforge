package org.devlive.metaforge.spi.model;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.view.EntityView;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ManagementEntry
{
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String id;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String name;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String type;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private long size = -1;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private Long ttl;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private Long modifiedAt;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private Map<String, String> attributes = new LinkedHashMap<>();
}
