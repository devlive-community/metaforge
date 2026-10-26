package org.devlive.metaforge.spi.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Pagination
{
    private int size; // 每页显示的记录数
    private int page; // 当前页
    private int total; // 总记录数
    private int pages; // 总页数
    private boolean hasPrevious; // 是否有上一页
    private boolean hasNext; // 是否有下一页
    private int startIndex; // 开始索引
    private int endIndex; // 结束索引

    private Pagination(int size, int page)
    {
        this.size = size;
        this.page = page;
    }

    public static Pagination create(int size, int page)
    {
        return new Pagination(size, page);
    }

    public Pagination total(int total)
    {
        this.total = total;
        return this;
    }

    public Pagination hasPrevious(boolean hasPrevious)
    {
        this.hasPrevious = hasPrevious;
        return this;
    }

    public Pagination hasNext(boolean hasNext)
    {
        this.hasNext = hasNext;
        return this;
    }

    public Pagination startIndex(int startIndex)
    {
        this.startIndex = startIndex;
        return this;
    }

    public Pagination endIndex(int endIndex)
    {
        this.endIndex = endIndex;
        return this;
    }

    public Pagination pages(int pages)
    {
        this.pages = pages;
        return this;
    }
}
