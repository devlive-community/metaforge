package org.devlive.metaforge.spi.model;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.view.EntityView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ManagementDetail
{
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private ManagementEntry entry;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String content;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String encoding;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private boolean truncated;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private List<Map<String, String>> rows = new ArrayList<>();
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String nextCursor;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private boolean hasMore;
}
