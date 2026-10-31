package org.devlive.metaforge.spi.model;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.view.EntityView;

@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ManagementRequest
{
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String namespace;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String prefix = "";
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String cursor = "";
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private String key;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private int limit = 50;
    @JsonView({EntityView.UserView.class, EntityView.AdminView.class})
    private boolean preview;

    public void validate()
    {
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Batch size must be between 1 and 200");
        }
        if ((namespace != null && namespace.length() > 1024) || (prefix != null && prefix.length() > 4096)
                || (key != null && key.length() > 16384) || (cursor != null && cursor.length() > 131072)) {
            throw new IllegalArgumentException("Management request is too large");
        }
        prefix = prefix == null ? "" : prefix;
        cursor = cursor == null ? "" : cursor;
    }
}
