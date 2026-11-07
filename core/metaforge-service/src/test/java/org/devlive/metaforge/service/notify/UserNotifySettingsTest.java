package org.devlive.metaforge.service.notify;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.notify.NotifyService;
import org.devlive.metaforge.notify.model.NotifyRequest;
import org.devlive.metaforge.notify.model.NotifyResponse;
import org.devlive.metaforge.service.entity.NotificationEntity;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;
import org.devlive.metaforge.service.repository.NotificationRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserNotifySettingsTest
{
    private static final String ROBOT = "DingTalk";

    private final List<NotificationEntity> saved = new ArrayList<>();
    private final List<NotifyRequest> sent = new ArrayList<>();
    private UserEntity stored;
    private UserNotifySettings settings;

    /** One plugin channel with a required secret and an optional URL. */
    private final class FakeChannels
            extends NotificationChannels
    {
        private FakeChannels()
        {
            super(null);
        }

        @Override
        public List<Channel> list()
        {
            return List.of(new Channel(INTERNAL, true, List.of()),
                    new Channel(ROBOT, false, List.of(
                            new Field("accessToken", "Access token", "SECRET", true, null, null),
                            new Field("callback", "Callback", "URL", false, null, null))));
        }

        @Override
        public Optional<NotifyService> service(String type)
        {
            if (!ROBOT.equals(type)) {
                return Optional.empty();
            }
            return Optional.of(request -> {
                sent.add(request);
                NotifyResponse response = new NotifyResponse();
                response.setSuccessful(true);
                return response;
            });
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T fake(Class<T> type)
    {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "findByCode":
                    return Optional.ofNullable(stored);
                case "save":
                    if (args[0] instanceof NotificationEntity) {
                        saved.add((NotificationEntity) args[0]);
                    }
                    else {
                        stored = (UserEntity) args[0];
                    }
                    return args[0];
                case "hashCode":
                    return 0;
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return type.getSimpleName();
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
    }

    @Before
    public void setUp()
    {
        stored = new UserEntity();
        stored.setId(1L);
        stored.setCode("u1");
        UserRepository users = fake(UserRepository.class);
        NotificationChannels channels = new FakeChannels();
        settings = new UserNotifySettings(users, channels, new NotificationDispatcher(users, fake(NotificationRepository.class), channels));
    }

    private static UserEntity login(String... roles)
    {
        UserEntity user = new UserEntity();
        user.setCode("u1");
        user.setRoles(java.util.Arrays.stream(roles).map(code -> RoleEntity.builder().code(code).build()).collect(Collectors.toSet()));
        return user;
    }

    private static UserNotifySettings.ChannelSetting robot(boolean enabled, Map<String, String> configure, Set<String> services)
    {
        return new UserNotifySettings.ChannelSetting(ROBOT, enabled, services, configure, null);
    }

    @Test
    public void enabledChannelNeedsItsRequiredSettings()
    {
        CommonResponse<?> response = settings.save(login(), List.of(robot(true, Map.of(), Set.of("DATASET_CREATED"))));
        Assert.assertFalse(response.getStatus());
        Assert.assertTrue(response.getMessage().toString(), response.getMessage().toString().contains("required"));
    }

    @Test
    public void secretsAreWriteOnlyAndEmptyKeepsTheStoredValue()
    {
        Assert.assertTrue(settings.save(login(), List.of(robot(true, Map.of("accessToken", "tok-1"), Set.of("DATASET_CREATED")))).getStatus());
        Assert.assertEquals("tok-1", stored.getNotifyConfigure().get(0).getConfigure().get("accessToken"));

        UserNotifySettings.ChannelSetting read = settings.settings(login()).getData().stream()
                .filter(channel -> ROBOT.equals(channel.getType())).findFirst().orElseThrow();
        Assert.assertEquals("", read.getConfigure().get("accessToken"));
        Assert.assertEquals(Set.of("accessToken"), read.getConfigured());

        Assert.assertTrue(settings.save(login(), List.of(robot(true, Map.of("accessToken", ""), Set.of("DATASET_UPDATED")))).getStatus());
        NotificationTypeEntity entry = stored.getNotifyConfigure().get(0);
        Assert.assertEquals("tok-1", entry.getConfigure().get("accessToken"));
        Assert.assertEquals(Set.of("DATASET_UPDATED"), entry.getServices());
    }

    @Test
    public void disabledChannelKeepsSettingsButDropsSubscriptions()
    {
        settings.save(login(), List.of(robot(true, Map.of("accessToken", "tok-1"), Set.of("DATASET_CREATED"))));
        Assert.assertTrue(settings.save(login(), List.of(robot(false, Map.of(), Set.of("DATASET_CREATED")))).getStatus());
        NotificationTypeEntity entry = stored.getNotifyConfigure().get(0);
        Assert.assertFalse(entry.isEnabled());
        Assert.assertTrue(entry.getServices().isEmpty());
        Assert.assertEquals("tok-1", entry.getConfigure().get("accessToken"));
    }

    @Test
    public void unknownChannelsScenesAndBadAddressesAreRejected()
    {
        Assert.assertFalse(settings.save(login(), List.of(new UserNotifySettings.ChannelSetting("Nope", true, Set.of(), Map.of(), null))).getStatus());
        Assert.assertFalse(settings.save(login(), List.of(robot(true, Map.of("accessToken", "t"), Set.of("NOPE")))).getStatus());
        Assert.assertFalse(settings.save(login(), List.of(robot(true, Map.of("accessToken", "t", "callback", "ftp://x"), Set.of()))).getStatus());
    }

    @Test
    public void adminScenesAreOfferedToAdministratorsOnly()
    {
        Assert.assertFalse(settings.save(login(), List.of(robot(true, Map.of("accessToken", "t"), Set.of("PLUGIN_INSTALLED")))).getStatus());
        Assert.assertTrue(settings.save(login("ADMIN"), List.of(robot(true, Map.of("accessToken", "t"), Set.of("PLUGIN_INSTALLED")))).getStatus());
        Assert.assertTrue(settings.catalog(login()).getCategories().stream().noneMatch(category -> "PLUGIN".equals(category.getKey())));
        Assert.assertTrue(settings.catalog(login("ADMIN")).getCategories().stream().anyMatch(category -> "PLUGIN".equals(category.getKey())));
    }

    @Test
    public void legacySubscriptionsReadAsScenes()
    {
        stored.setNotifyConfigure(new ArrayList<>(List.of(NotificationTypeEntity.builder()
                .type(NotificationChannels.INTERNAL).enabled(true).services(Set.of("DYNAMIC", "SYNCDATA")).build())));
        Set<String> services = settings.settings(login()).getData().get(0).getServices();
        Assert.assertEquals(Set.of("DATASET_CREATED", "DATASET_UPDATED", "DATASET_SYNC_SUCCEEDED", "DATASET_SYNC_FAILED"), services);
        Assert.assertEquals(List.of(NotificationChannels.INTERNAL), NotificationDispatcher.subscribed(stored, NotificationScene.DATASET_SYNC_FAILED));
        Assert.assertTrue(NotificationDispatcher.subscribed(stored, NotificationScene.SOURCE_CREATED).isEmpty());
    }

    @Test
    public void testSendUsesTheStoredSecretWhenLeftEmpty()
    {
        settings.save(login(), List.of(robot(true, Map.of("accessToken", "tok-1"), Set.of())));
        Assert.assertTrue(settings.test(login(), robot(true, Map.of(), Set.of())).getStatus());
        Assert.assertEquals("tok-1", sent.get(0).getConfigure().get("accessToken"));

        Assert.assertTrue(settings.test(login(), new UserNotifySettings.ChannelSetting(NotificationChannels.INTERNAL, true, Set.of(), Map.of(), null)).getStatus());
        Assert.assertEquals(1, saved.size());
    }
}
