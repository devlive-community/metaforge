package org.devlive.metaforge.service.notify;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.notify.model.NotifyRequest;
import org.devlive.metaforge.notify.model.NotifyResponse;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;
import org.devlive.metaforge.service.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A user's notification settings: which channels are on, each channel's own settings, and the
 * scenes each enabled channel receives. Only enabled channels hold subscriptions, and secret
 * settings are write-only: reads say whether they are set, an empty value on save keeps them.
 */
@Slf4j
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class UserNotifySettings
{
    static final int MAX_VALUE_LENGTH = 2000;

    private final UserRepository users;
    private final NotificationChannels channels;
    private final NotificationDispatcher dispatcher;

    public UserNotifySettings(UserRepository users, NotificationChannels channels, NotificationDispatcher dispatcher)
    {
        this.users = users;
        this.channels = channels;
        this.dispatcher = dispatcher;
    }

    /** One channel as the settings page sees it. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Plain request and response object")
    public static class ChannelSetting
    {
        private String type;
        private boolean enabled;
        private Set<String> services;
        private Map<String, String> configure;
        /** Secret fields that hold a value; their values are never sent. */
        private Set<String> configured;
    }

    @Data
    @AllArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Plain response object")
    public static class Catalog
    {
        private List<NotificationChannels.Channel> channels;
        private List<Category> categories;
    }

    @Data
    @AllArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Plain response object")
    public static class Category
    {
        private String key;
        private List<String> scenes;
    }

    /** Channels and the scene categories this user may subscribe to. */
    public Catalog catalog(UserEntity user)
    {
        boolean admin = isAdmin(user);
        List<Category> categories = Arrays.stream(NotificationScene.Category.values())
                .map(category -> new Category(category.name(), Arrays.stream(NotificationScene.values())
                        .filter(scene -> scene.getCategory() == category && (admin || !scene.isAdmin()))
                        .map(Enum::name)
                        .collect(Collectors.toList())))
                .filter(category -> !category.getScenes().isEmpty())
                .collect(Collectors.toList());
        return new Catalog(channels.list(), categories);
    }

    /** The signed-in user's settings as stored, secrets masked. */
    public CommonResponse<List<ChannelSetting>> settings(UserEntity login)
    {
        return users.findByCode(login.getCode())
                .map(user -> {
                    user.setRoles(login.getRoles());
                    return CommonResponse.success(read(user));
                })
                .orElseGet(() -> CommonResponse.failure("User not found"));
    }

    List<ChannelSetting> read(UserEntity user)
    {
        Map<String, NotificationTypeEntity> stored = stored(user);
        Set<String> allowed = allowedScenes(user);
        return channels.list().stream().map(channel -> {
            NotificationTypeEntity saved = stored.get(channel.getType());
            Map<String, String> values = saved == null || saved.getConfigure() == null ? Map.of() : saved.getConfigure();
            Map<String, String> configure = new LinkedHashMap<>();
            Set<String> configured = new LinkedHashSet<>();
            for (NotificationChannels.Field field : channel.getFields()) {
                String value = values.get(field.getName());
                if (field.isSecret()) {
                    configure.put(field.getName(), "");
                    if (StringUtils.isNotBlank(value)) {
                        configured.add(field.getName());
                    }
                }
                else {
                    configure.put(field.getName(), StringUtils.defaultString(value));
                }
            }
            boolean enabled = saved != null && saved.isEnabled();
            Set<String> services = enabled ? NotificationScene.normalize(saved.getServices()) : new LinkedHashSet<>();
            services.retainAll(allowed);
            return new ChannelSetting(channel.getType(), enabled, services, configure, configured);
        }).collect(Collectors.toList());
    }

    public CommonResponse<List<ChannelSetting>> save(UserEntity login, List<ChannelSetting> request)
    {
        Optional<UserEntity> found = users.findByCode(login.getCode());
        if (found.isEmpty()) {
            return CommonResponse.failure("User not found");
        }
        UserEntity user = found.get();
        // Roles are not stored on the user row; the signed-in principal carries them.
        user.setRoles(login.getRoles());
        Map<String, NotificationTypeEntity> stored = stored(user);
        Set<String> allowed = allowedScenes(login);
        Map<String, ChannelSetting> incoming = new LinkedHashMap<>();
        for (ChannelSetting setting : Optional.ofNullable(request).orElse(List.of())) {
            if (setting == null || setting.getType() == null) {
                continue;
            }
            if (channels.find(setting.getType()).isEmpty()) {
                return CommonResponse.failure("Unknown notification channel: " + setting.getType());
            }
            incoming.put(setting.getType(), setting);
        }
        for (ChannelSetting setting : incoming.values()) {
            NotificationChannels.Channel channel = channels.find(setting.getType()).orElseThrow();
            NotificationTypeEntity previous = stored.get(channel.getType());
            Map<String, String> before = previous == null || previous.getConfigure() == null ? Map.of() : previous.getConfigure();
            Map<String, String> values = new LinkedHashMap<>();
            Map<String, String> given = Optional.ofNullable(setting.getConfigure()).orElse(Map.of());
            for (NotificationChannels.Field field : channel.getFields()) {
                String value = StringUtils.trimToEmpty(given.get(field.getName()));
                // Secrets are never sent back to the page: leaving one empty keeps the stored value.
                if (field.isSecret() && value.isEmpty()) {
                    value = StringUtils.defaultString(before.get(field.getName()));
                }
                if (value.length() > MAX_VALUE_LENGTH) {
                    return CommonResponse.failure(channel.getType() + ": " + field.getLabel() + " is too long");
                }
                if ("URL".equals(field.getType()) && !value.isEmpty() && !value.matches("(?i)^https?://\\S+$")) {
                    return CommonResponse.failure(channel.getType() + ": " + field.getLabel() + " must be an http(s) address");
                }
                if (setting.isEnabled() && field.isRequired() && value.isEmpty()) {
                    return CommonResponse.failure(channel.getType() + ": " + field.getLabel() + " is required");
                }
                if (!value.isEmpty()) {
                    values.put(field.getName(), value);
                }
            }
            Set<String> services = new LinkedHashSet<>();
            // A disabled channel receives nothing, so it keeps its settings but no subscriptions.
            if (setting.isEnabled()) {
                Set<String> requested = Optional.ofNullable(setting.getServices()).orElse(Set.of());
                Set<String> unknown = requested.stream().filter(scene -> !allowed.contains(scene)).collect(Collectors.toSet());
                if (!unknown.isEmpty()) {
                    return CommonResponse.failure("Unknown notification scenes: " + String.join(", ", unknown));
                }
                services.addAll(requested);
            }
            stored.put(channel.getType(), NotificationTypeEntity.builder()
                    .type(channel.getType()).enabled(setting.isEnabled()).services(services).configure(values).build());
        }
        user.setNotifyConfigure(new ArrayList<>(stored.values()));
        users.save(user);
        return CommonResponse.success(read(user));
    }

    /** Sends a test message with the given settings; empty secrets fall back to the stored ones. */
    public CommonResponse<String> test(UserEntity login, ChannelSetting setting)
    {
        if (setting == null || setting.getType() == null) {
            return CommonResponse.failure("Unknown notification channel");
        }
        Optional<NotificationChannels.Channel> channel = channels.find(setting.getType());
        Optional<UserEntity> user = users.findByCode(login.getCode());
        if (channel.isEmpty() || user.isEmpty()) {
            return CommonResponse.failure("Unknown notification channel: " + setting.getType());
        }
        if (channel.get().isBuiltin()) {
            dispatcher.saveInternal(user.get(), null, "通知测试", "这是一条来自 MetaForge 的测试通知。", null);
            return CommonResponse.success("sent");
        }
        NotificationTypeEntity previous = stored(user.get()).get(setting.getType());
        Map<String, String> before = previous == null || previous.getConfigure() == null ? Map.of() : previous.getConfigure();
        Map<String, String> values = new LinkedHashMap<>();
        for (NotificationChannels.Field field : channel.get().getFields()) {
            String value = StringUtils.trimToEmpty(Optional.ofNullable(setting.getConfigure()).orElse(Map.of()).get(field.getName()));
            values.put(field.getName(), value.isEmpty() && field.isSecret() ? StringUtils.defaultString(before.get(field.getName())) : value);
        }
        NotifyRequest request = new NotifyRequest();
        request.setTitle("MetaForge 通知测试");
        request.setContent("这是一条来自 MetaForge 的测试通知，收到说明 " + setting.getType() + " 渠道配置正确。");
        request.setConfigure(values);
        try {
            NotifyResponse response = channels.service(setting.getType()).map(service -> service.send(request)).orElse(null);
            if (response == null) {
                return CommonResponse.failure("The notification plugin is not available");
            }
            return response.getSuccessful() ? CommonResponse.success("sent") : CommonResponse.failure(StringUtils.defaultIfBlank(response.getMessage(), "Sending failed"));
        }
        catch (RuntimeException e) {
            log.warn("Test notification on {} failed: {}", setting.getType(), e.getClass().getSimpleName());
            return CommonResponse.failure("Sending failed: " + StringUtils.defaultString(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    static boolean isAdmin(UserEntity user)
    {
        return user != null && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role != null && role.getCode() != null && role.getCode().contains("ADMIN"));
    }

    private static Set<String> allowedScenes(UserEntity user)
    {
        boolean admin = isAdmin(user);
        return Arrays.stream(NotificationScene.values()).filter(scene -> admin || !scene.isAdmin()).map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<String, NotificationTypeEntity> stored(UserEntity user)
    {
        Map<String, NotificationTypeEntity> stored = new LinkedHashMap<>();
        for (NotificationTypeEntity entry : Optional.ofNullable(user.getNotifyConfigure()).orElse(List.of())) {
            if (entry != null && entry.getType() != null && !stored.containsKey(entry.getType())) {
                stored.put(entry.getType(), entry);
            }
        }
        return stored;
    }
}
