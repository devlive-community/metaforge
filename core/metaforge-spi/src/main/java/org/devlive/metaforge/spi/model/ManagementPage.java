package org.devlive.metaforge.spi.model;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.view.EntityView;

import java.util.ArrayList;
import java.util.List;

@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ManagementPage
{
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private List<ManagementEntry> entries = new ArrayList<>();
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String nextCursor;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private boolean hasMore;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String namespace;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String prefix;
}
