package org.devlive.metaforge.server.notify;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.devlive.metaforge.service.notify.NotificationInbox;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.dialect.H2Dialect;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Runs the real changelog on H2, then checks the inbox only ever touches the signed-in user's rows. */
public class NotificationInboxTest
{
    private static final long ME = 1L;
    private static final long OTHER = 2L;

    private NamedParameterJdbcTemplate jdbc;
    private NotificationInbox inbox;

    @BeforeEach
    public void setUp()
            throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:inbox-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = source.getConnection()) {
            new Liquibase("db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))).update("");
        }
        jdbc = new NamedParameterJdbcTemplate(source);
        inbox = new NotificationInbox(jdbc, H2Dialect.INSTANCE);
        insert(ME, "a", "DATASET_SYNC_FAILED", "数据集「订单」同步失败", "x".repeat(1500), false, "2026-11-05 10:00:00");
        insert(ME, "b", "TEST", "通知测试", "这是一条测试通知", false, "2026-11-05 11:00:00");
        insert(ME, "c", "SYNCDATA", "旧的同步通知", "历史数据 100%_done", true, "2026-11-04 09:00:00");
        insert(ME, "d", "PASSWORD_CHANGED", "登录密码已修改", "你的登录密码已修改", true, "2026-11-03 09:00:00");
        insert(OTHER, "e", "TEST", "别人的通知", "不可见", false, "2026-11-05 12:00:00");
    }

    private void insert(long user, String code, String type, String name, String content, boolean read, String time)
    {
        jdbc.update("INSERT INTO metaforge_notification (code, name, content, type, is_read, user_id, create_time, entity_exists) "
                        + "VALUES (:code, :name, :content, :type, :read, :user, :time, :exists)",
                new MapSqlParameterSource("code", code).addValue("name", name).addValue("content", content).addValue("type", type)
                        .addValue("read", read).addValue("user", user).addValue("time", Timestamp.valueOf(time)).addValue("exists", true));
    }

    private List<String> codes(NotificationInbox.Query query)
    {
        return inbox.page(ME, query).getItems().stream().map(NotificationInbox.Item::getCode).collect(Collectors.toList());
    }

    @Test
    public void listsOnlyMineNewestFirstAndFiltersByStateCategoryAndWords()
    {
        NotificationInbox.Query all = new NotificationInbox.Query();
        Assertions.assertEquals(List.of("b", "a", "c", "d"), codes(all));
        Assertions.assertEquals(1500, inbox.page(ME, all).getItems().get(1).getContent().length());

        NotificationInbox.Query unread = new NotificationInbox.Query();
        unread.setRead(false);
        Assertions.assertEquals(List.of("b", "a"), codes(unread));

        NotificationInbox.Query dataset = new NotificationInbox.Query();
        dataset.setCategory("DATASET");
        Assertions.assertEquals(List.of("a", "c"), codes(dataset), "old operation names count as datasets");

        NotificationInbox.Query system = new NotificationInbox.Query();
        system.setCategory("SYSTEM");
        Assertions.assertEquals(List.of("b"), codes(system));

        NotificationInbox.Query words = new NotificationInbox.Query();
        words.setKeyword("%_");
        Assertions.assertEquals(List.of("c"), codes(words), "wildcards are matched literally");

        NotificationInbox.Query paged = new NotificationInbox.Query();
        paged.setSize(3);
        paged.setPage(2);
        Assertions.assertEquals(List.of("d"), codes(paged));
        Assertions.assertEquals(4, inbox.page(ME, paged).getTotal());
    }

    @Test
    public void summaryCountsByCategory()
    {
        NotificationInbox.Summary summary = inbox.summary(ME);
        Assertions.assertEquals(4, summary.getTotal());
        Assertions.assertEquals(2, summary.getUnread());
        Assertions.assertEquals(Long.valueOf(2), summary.getCategories().get("DATASET"));
        Assertions.assertEquals(Long.valueOf(1), summary.getCategories().get("SYSTEM"));
        Assertions.assertEquals(Long.valueOf(1), summary.getCategories().get("ACCOUNT"));
        Assertions.assertEquals(Long.valueOf(0), summary.getCategories().get("PLUGIN"));
    }

    @Test
    public void readStateAndRemovalNeverReachAnotherUsersNotifications()
    {
        Assertions.assertFalse(inbox.mark(ME, "e", true));
        Assertions.assertFalse(inbox.delete(ME, "e"));
        Assertions.assertTrue(inbox.find(ME, "e").isEmpty());

        Assertions.assertTrue(inbox.mark(ME, "c", false));
        Assertions.assertEquals(3, inbox.unread(ME));
        Assertions.assertEquals(1, inbox.markAllRead(ME, "DATASET") - 1);
        Assertions.assertEquals(1, inbox.unread(ME));
        Assertions.assertEquals(1, inbox.markAllRead(ME, null));
        Assertions.assertEquals(0, inbox.unread(ME));
        Assertions.assertEquals(1, inbox.unread(OTHER));

        Assertions.assertTrue(inbox.delete(ME, "a"));
        Assertions.assertEquals(3, inbox.deleteRead(ME));
        Assertions.assertEquals(0, inbox.summary(ME).getTotal());
        Assertions.assertEquals(1, inbox.summary(OTHER).getTotal());
    }
}
