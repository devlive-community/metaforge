package org.devlive.metaforge.spi.adapter;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Batch writes follow the target's transaction support: analytical engines such as ClickHouse have none. */
public class JdbcBatchWriterTest
{
    /** Records the calls a writer makes on a stubbed JDBC connection. */
    private static final class Driver
    {
        final List<String> calls = new ArrayList<>();
        final AtomicInteger batches = new AtomicInteger();
        boolean transactions;
        boolean commitUnsupported;
        boolean batchFails;
        boolean autoCommit = true;

        Connection connection()
        {
            PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {PreparedStatement.class}, (proxy, method, args) -> {
                if (method.getName().equals("executeBatch")) {
                    batches.incrementAndGet();
                    if (batchFails) {
                        throw new SQLException("constraint violated");
                    }
                    return new int[0];
                }
                return null;
            });
            DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DatabaseMetaData.class},
                    (proxy, method, args) -> method.getName().equals("supportsTransactions") ? transactions : null);
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                calls.add(method.getName() + (args == null ? "" : List.of(args)));
                switch (method.getName()) {
                    case "getMetaData":
                        return metadata;
                    case "getAutoCommit":
                        return autoCommit;
                    case "setAutoCommit":
                        autoCommit = (Boolean) args[0];
                        return null;
                    case "prepareStatement":
                        return statement;
                    case "commit":
                        if (commitUnsupported) {
                            throw new SQLFeatureNotSupportedException("commit() is not supported.");
                        }
                        return null;
                    default:
                        return null;
                }
            });
        }
    }

    private static void write(BatchWriter writer, int rows)
    {
        for (int index = 0; index < rows; index++) {
            writer.addRow(List.of(index));
        }
        writer.close();
    }

    @Test
    public void targetsWithoutTransactionsWriteWithAutoCommit()
    {
        Driver driver = new Driver();
        AtomicBoolean released = new AtomicBoolean();
        BatchWriter writer = JdbcStreamAdapter.batchWriter(driver.connection(), () -> released.set(true), "db", "events", List.of("id"), 2);
        write(writer, 5);
        Assert.assertEquals(5, writer.writtenCount());
        Assert.assertEquals(3, driver.batches.get());
        Assert.assertFalse(driver.calls.contains("commit"));
        Assert.assertTrue(driver.calls.stream().noneMatch(call -> call.startsWith("setAutoCommit")));
        Assert.assertTrue(released.get());
    }

    @Test
    public void transactionalTargetsCommitEachBatchAndRestoreAutoCommit()
    {
        Driver driver = new Driver();
        driver.transactions = true;
        BatchWriter writer = JdbcStreamAdapter.batchWriter(driver.connection(), () -> { }, "db", "events", List.of("id"), 2);
        write(writer, 3);
        Assert.assertEquals(3, writer.writtenCount());
        Assert.assertEquals(2, driver.calls.stream().filter("commit"::equals).count());
        Assert.assertTrue(driver.calls.contains("setAutoCommit[false]"));
        Assert.assertTrue(driver.autoCommit);
    }

    @Test
    public void aFailedBatchIsRolledBackNotCountedAndNotSentAgainOnClose()
    {
        Driver driver = new Driver();
        driver.transactions = true;
        AtomicBoolean released = new AtomicBoolean();
        BatchWriter writer = JdbcStreamAdapter.batchWriter(driver.connection(), () -> released.set(true), "db", "events", List.of("id"), 2);
        writer.addRow(List.of(1));
        writer.addRow(List.of(2));
        Assert.assertEquals(2, writer.writtenCount());
        driver.batchFails = true;
        writer.addRow(List.of(3));
        Assert.assertThrows(IllegalStateException.class, () -> writer.addRow(List.of(4)));
        Assert.assertEquals(2, writer.writtenCount());
        Assert.assertTrue(driver.calls.contains("rollback"));
        Assert.assertEquals(2, driver.batches.get());
        writer.close();
        Assert.assertEquals("close does not retry the failed batch", 2, driver.batches.get());
        Assert.assertEquals(2, writer.writtenCount());
        Assert.assertTrue(released.get());
    }

    @Test
    public void flushCommitsAPartialBatchAndDoesNothingWhenNoneIsPending()
    {
        Driver driver = new Driver();
        driver.transactions = true;
        BatchWriter writer = JdbcStreamAdapter.batchWriter(driver.connection(), () -> { }, "db", "events", List.of("id"), 10);
        writer.addRow(List.of(1));
        writer.addRow(List.of(2));
        Assert.assertEquals(0, writer.writtenCount());
        writer.flush();
        Assert.assertEquals(2, writer.writtenCount());
        Assert.assertEquals(1, driver.batches.get());
        writer.flush();
        Assert.assertEquals(1, driver.batches.get());
        writer.close();
        Assert.assertEquals(1, driver.batches.get());
        Assert.assertEquals(2, writer.writtenCount());
    }

    @Test
    public void driversThatRejectCommitKeepWriting()
    {
        Driver driver = new Driver();
        driver.transactions = true;
        driver.commitUnsupported = true;
        BatchWriter writer = JdbcStreamAdapter.batchWriter(driver.connection(), () -> { }, "db", "events", List.of("id"), 2);
        write(writer, 5);
        Assert.assertEquals(5, writer.writtenCount());
        Assert.assertEquals(3, driver.batches.get());
        // The first unsupported commit switches the writer to auto-commit; it is not retried.
        Assert.assertEquals(1, driver.calls.stream().filter("commit"::equals).count());
        Assert.assertTrue(driver.autoCommit);
    }
}
