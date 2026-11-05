package org.devlive.metaforge.service.notify;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.devlive.metaforge.notify.NotifyField;
import org.devlive.metaforge.notify.NotifyFieldType;
import org.devlive.metaforge.notify.NotifyService;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The channels a user can be notified on: the built-in site channel plus every installed and
 * enabled notify plugin, each with the settings it asks every user for.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The plugin manager is a Spring-managed singleton")
public class NotificationChannels
{
    public static final String INTERNAL = "Internal";

    private final PluginManager plugins;

    public NotificationChannels(PluginManager plugins)
    {
        this.plugins = plugins;
    }

    @Data
    @AllArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Plain response object")
    public static class Channel
    {
        private String type;
        private boolean builtin;
        private List<Field> fields;
    }

    @Data
    @AllArgsConstructor
    public static class Field
    {
        private String name;
        private String label;
        private String type;
        private boolean required;
        private String description;
        private String placeholder;

        public boolean isSecret()
        {
            return NotifyFieldType.SECRET.name().equals(type);
        }
    }

    public List<Channel> list()
    {
        List<Channel> channels = new ArrayList<>();
        channels.add(new Channel(INTERNAL, true, List.of()));
        plugins.getPluginInfos().stream()
                .filter(info -> info.getType() == PluginType.NOTIFY)
                .map(PluginMetadata::getName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(name -> service(name).ifPresent(service -> channels.add(new Channel(name, false, fields(service)))));
        return channels;
    }

    public Optional<Channel> find(String type)
    {
        return list().stream().filter(channel -> channel.getType().equals(type)).findFirst();
    }

    /** The plugin behind a channel, absent for the site channel and for uninstalled or disabled plugins. */
    public Optional<NotifyService> service(String type)
    {
        if (type == null || INTERNAL.equals(type)) {
            return Optional.empty();
        }
        return plugins.getPlugin(type).flatMap(plugin -> Optional.ofNullable(plugin.getService(NotifyService.class)));
    }

    private static List<Field> fields(NotifyService service)
    {
        List<NotifyField> declared;
        try {
            declared = service.fields();
        }
        catch (RuntimeException | LinkageError e) {
            // A plugin built against the older interface declares no settings.
            declared = List.of();
        }
        return declared.stream()
                .map(field -> new Field(field.getName(), field.getLabel(), field.getType().name(), field.getRequired(), field.getDescription(), field.getPlaceholder()))
                .collect(Collectors.toList());
    }
}
