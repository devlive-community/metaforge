package org.devlive.metaforge.spi.adapter;

import java.util.List;

public interface BatchWriter
        extends AutoCloseable
{
    void addRow(List<?> row);

    /**
     * 立即提交已攒下的行。没有待提交的行时什么也不做。
     * Commit the rows added so far; does nothing when none are pending.
     */
    void flush();

    long writtenCount();

    /**
     * 写入失败的一批是否不留下任何行，因而可以安全地重写其中一部分。
     * Whether a failed write leaves none of its rows behind, so part of it can safely be written again.
     */
    default boolean failedWritesLeaveNoRows()
    {
        return false;
    }

    @Override
    void close();
}
