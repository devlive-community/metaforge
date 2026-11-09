package org.devlive.metaforge.service.enums;

import java.util.Optional;

/**
 * What a user may do with a data screen shared with them, from least to most. The owner is above all
 * of these: only the owner deletes a screen or changes how it is shared.
 */
public enum SharePermission
{
    /** See the published screen, limited to the widgets the owner chose. */
    VIEW,
    /** Also open the designer and save drafts; the published screen stays as it was. */
    EDIT,
    /** Also publish drafts. */
    PUBLISH,
    /** The owner. Never granted; resolved for the screen's own user. */
    OWNER;

    public boolean atLeast(SharePermission other)
    {
        return ordinal() >= other.ordinal();
    }

    /** A grantable permission by name, ignoring case; OWNER is never grantable. */
    public static Optional<SharePermission> grantable(String value)
    {
        for (SharePermission permission : values()) {
            if (permission != OWNER && permission.name().equalsIgnoreCase(value == null ? "" : value.trim())) {
                return Optional.of(permission);
            }
        }
        return Optional.empty();
    }
}
