package org.devlive.metaforge.server.persistence;

import org.devlive.metaforge.common.enums.DataSetState;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.server.support.PersistenceContext;
import org.devlive.metaforge.server.support.TestDatabase;
import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;
import org.devlive.metaforge.service.persistence.References;
import org.devlive.metaforge.service.repository.DataSetColumnRepository;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.repository.ScheduledHistoryRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.mapping.Table;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every mapped entity matches the migrated schema, and the value types that need conversions
 * (JSON objects, text-backed lists, flags, CLOB text) round-trip on the selected database.
 */
public class EntityMappingTest
{
    private static PersistenceContext context;

    @BeforeAll
    public static void startDatabase()
            throws Exception
    {
        context = PersistenceContext.start(TestDatabase.selectedSpec());
    }

    @AfterAll
    public static void stopDatabase()
    {
        context.close();
    }

    @Test
    public void everyMappedColumnExistsInTheMigratedSchema()
            throws Exception
    {
        Map<String, Set<String>> schema = schema();
        RelationalMappingContext mapping = context.bean(RelationalMappingContext.class);
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Table.class));
        List<String> problems = new ArrayList<>();
        int entities = 0;
        for (BeanDefinition definition : scanner.findCandidateComponents("org.devlive.metaforge.service.entity")) {
            entities++;
            RelationalPersistentEntity<?> entity = mapping.getRequiredPersistentEntity(Class.forName(definition.getBeanClassName()));
            String table = entity.getTableName().getReference().toLowerCase(Locale.ROOT);
            Set<String> columns = schema.get(table);
            if (columns == null) {
                problems.add("missing table " + table);
                continue;
            }
            entity.doWithProperties((RelationalPersistentProperty property) -> {
                if (property.isEntity()) {
                    problems.add(table + "." + property.getName() + " is mapped as a nested aggregate");
                }
                else if (!columns.contains(property.getColumnName().getReference().toLowerCase(Locale.ROOT))) {
                    problems.add("missing column " + table + "." + property.getColumnName().getReference());
                }
            });
        }
        assertEquals(21, entities);
        assertEquals(List.of(), problems);
    }

    @Test
    public void everyTableCanBeReadThroughItsEntity()
            throws Exception
    {
        JdbcAggregateOperations template = context.bean(JdbcAggregateOperations.class);
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Table.class));
        for (BeanDefinition definition : scanner.findCandidateComponents("org.devlive.metaforge.service.entity")) {
            // seeded rows (menus, roles, functions, schedules) exercise the vendor value types
            template.findAll(Class.forName(definition.getBeanClassName())).forEach(entity -> assertTrue(entity != null));
        }
    }

    @Test
    public void convertedValuesRoundTrip()
    {
        UserEntity user = new UserEntity();
        user.setUsername("mapper-" + System.nanoTime());
        AvatarEntity avatar = new AvatarEntity();
        avatar.setPath("avatars/权限.png");
        user.setAvatarConfigure(avatar);
        NotificationTypeEntity notify = new NotificationTypeEntity();
        notify.setType("email");
        user.setNotifyConfigure(List.of(notify));
        user.setChatConfigure("x".repeat(20000));
        UserEntity saved = context.bean(UserRepository.class).save(user);
        UserEntity loaded = context.bean(UserRepository.class).findById(saved.getId()).orElseThrow();
        assertEquals("avatars/权限.png", loaded.getAvatarConfigure().getPath());
        assertEquals("email", loaded.getNotifyConfigure().get(0).getType());
        assertEquals(20000, loaded.getChatConfigure().length());
        assertTrue(loaded.isActive());
        assertFalse(loaded.isSystem());

        SourceEntity source = new SourceEntity();
        source.setName("source");
        source.setType("MySQL");
        source.setHost("localhost");
        source.setPort(3306);
        source.setPublish(true);
        source.setUser(saved);
        SourceEntity savedSource = context.bean(SourceRepository.class).save(source);
        assertEquals(saved.getId(), savedSource.getUserId());

        DataSetEntity dataset = new DataSetEntity();
        dataset.setName("dataset");
        dataset.setState(List.of(DataSetState.METADATA_SUCCESS, DataSetState.TABLE_SUCCESS));
        dataset.setTotalRows(42L);
        dataset.setSource(savedSource);
        dataset.setUser(saved);
        DataSetEntity savedDataset = context.bean(DataSetRepository.class).save(dataset);
        DataSetEntity loadedDataset = context.bean(References.class).load(context.bean(DataSetRepository.class).findById(savedDataset.getId()).orElseThrow());
        assertEquals(List.of(DataSetState.METADATA_SUCCESS, DataSetState.TABLE_SUCCESS), loadedDataset.getState());
        assertEquals("source", loadedDataset.getSource().getName());
        assertTrue(loadedDataset.getSource().getPublish());
        assertEquals(saved.getUsername(), loadedDataset.getUser().getUsername());

        DataSetColumnEntity column = new DataSetColumnEntity();
        column.setName("amount");
        column.setPrimaryKey(true);
        column.setFormula("round(amount, 2)");
        column.setDataset(savedDataset);
        context.bean(DataSetColumnRepository.class).save(column);
        List<DataSetColumnEntity> columns = context.bean(DataSetColumnRepository.class).findAllByDatasetIdOrderByPositionAsc(savedDataset.getId());
        assertEquals(1, columns.size());
        assertTrue(columns.get(0).isPrimaryKey());
        assertEquals("round(amount, 2)", columns.get(0).getFormula());

        ScheduledHistoryEntity history = new ScheduledHistoryEntity();
        Properties info = new Properties();
        info.setProperty("rows", "7");
        history.setInfo(info);
        history.setState(RunState.SUCCESS);
        history.setSource(savedSource);
        ScheduledHistoryEntity savedHistory = context.bean(ScheduledHistoryRepository.class).save(history);
        ScheduledHistoryEntity loadedHistory = context.bean(ScheduledHistoryRepository.class).findById(savedHistory.getId()).orElseThrow();
        assertEquals("7", loadedHistory.getInfo().getProperty("rows"));
        assertEquals(RunState.SUCCESS, loadedHistory.getState());
    }

    private static Map<String, Set<String>> schema()
            throws Exception
    {
        Map<String, Set<String>> tables = new HashMap<>();
        try (Connection connection = context.bean(DataSource.class).getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet result = meta.getColumns(connection.getCatalog(), connection.getSchema(), null, null)) {
                while (result.next()) {
                    tables.computeIfAbsent(result.getString("TABLE_NAME").toLowerCase(Locale.ROOT), key -> new HashSet<>())
                            .add(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
        }
        return tables;
    }
}
