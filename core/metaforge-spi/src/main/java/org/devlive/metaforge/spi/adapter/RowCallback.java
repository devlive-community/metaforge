package org.devlive.metaforge.spi.adapter;

import java.sql.Statement;
import java.util.List;

public interface RowCallback
{
    /**
     * 在第一行之前回调一次，传入列元数据。默认空实现。
     * Called once before any row, carrying the column metadata of the source result.
     */
    default void onSchema(List<String> headers, List<String> types) {}

    /**
     * 在执行 query 之前回调一次，把底层 Statement 暴露给上层，
     * 让取消逻辑可以直接调用 {@link Statement#cancel()} 中断阻塞中的 fetch。
     * Called once right before the underlying query executes, exposing the JDBC Statement so the caller
     * can issue {@link Statement#cancel()} to interrupt a blocked fetch.
     */
    default void onStatement(Statement statement) {}

    /**
     * 每一行回调一次。
     * Called per row. Values are aligned to {@code headers} from {@link #onSchema}.
     */
    void onRow(List<Object> row);
}
