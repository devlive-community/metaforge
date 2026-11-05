package org.devlive.metaforge.service.enums;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Everything a user can be notified about, grouped by category. A user subscribes each enabled
 * channel to scenes; the web UI shows them as notify.category.* and notify.scene.*. Scenes marked
 * admin concern system administration and are offered to administrators only.
 */
public enum NotificationScene
{
    DATASET_CREATED(Category.DATASET),
    DATASET_UPDATED(Category.DATASET),
    DATASET_DELETED(Category.DATASET),
    DATASET_SYNC_SUCCEEDED(Category.DATASET),
    DATASET_SYNC_FAILED(Category.DATASET),

    SOURCE_CREATED(Category.SOURCE),
    SOURCE_UPDATED(Category.SOURCE),
    SOURCE_DELETED(Category.SOURCE),

    REPORT_CREATED(Category.REPORT),
    REPORT_DELETED(Category.REPORT),
    REPORT_SHARED(Category.REPORT),
    REPORT_UNSHARED(Category.REPORT),
    DASHBOARD_PUBLISHED(Category.REPORT),

    WORKFLOW_SUCCEEDED(Category.WORKFLOW),
    WORKFLOW_FAILED(Category.WORKFLOW),

    IMPORT_SUCCEEDED(Category.TRANSFER),
    IMPORT_FAILED(Category.TRANSFER),
    EXPORT_SUCCEEDED(Category.TRANSFER),
    EXPORT_FAILED(Category.TRANSFER),

    PASSWORD_CHANGED(Category.ACCOUNT),
    OAUTH_BOUND(Category.ACCOUNT),
    OAUTH_UNBOUND(Category.ACCOUNT),

    PLUGIN_INSTALLED(Category.PLUGIN, true),
    PLUGIN_UNINSTALLED(Category.PLUGIN, true),
    PLUGIN_STATE_CHANGED(Category.PLUGIN, true);

    public enum Category
    {
        DATASET, SOURCE, REPORT, WORKFLOW, TRANSFER, ACCOUNT, PLUGIN
    }

    // Subscriptions saved before scenes existed named only an operation, which always meant datasets.
    private static final Map<String, List<NotificationScene>> LEGACY = Map.of(
            "CREATED", List.of(DATASET_CREATED),
            "UPDATED", List.of(DATASET_UPDATED),
            "DELETED", List.of(DATASET_DELETED),
            "DYNAMIC", List.of(DATASET_CREATED, DATASET_UPDATED),
            "SYNCDATA", List.of(DATASET_SYNC_SUCCEEDED, DATASET_SYNC_FAILED));

    private final Category category;
    private final boolean admin;

    NotificationScene(Category category)
    {
        this(category, false);
    }

    NotificationScene(Category category, boolean admin)
    {
        this.category = category;
        this.admin = admin;
    }

    public Category getCategory()
    {
        return category;
    }

    public boolean isAdmin()
    {
        return admin;
    }

    public static Optional<NotificationScene> of(String name)
    {
        return Arrays.stream(values()).filter(scene -> scene.name().equals(name)).findFirst();
    }

    /** Scene names for saved subscriptions, upgrading old operation names; unknown names are dropped. */
    public static Set<String> normalize(Iterable<String> names)
    {
        Set<String> scenes = new LinkedHashSet<>();
        if (names == null) {
            return scenes;
        }
        for (String name : names) {
            if (name == null) {
                continue;
            }
            List<NotificationScene> legacy = LEGACY.get(name);
            if (legacy != null) {
                legacy.forEach(scene -> scenes.add(scene.name()));
            }
            else {
                of(name).ifPresent(scene -> scenes.add(scene.name()));
            }
        }
        return scenes;
    }
}
