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

    @Override
    void close();
}
