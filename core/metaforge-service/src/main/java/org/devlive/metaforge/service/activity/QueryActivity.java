package org.devlive.metaforge.service.activity;

import lombok.Value;

import java.util.List;

@Value
public class QueryActivity
{
    private int days;
    private long total;
    private List<Day> trend;
    private List<Source> sources;

    public QueryActivity(int days, long total, List<Day> trend, List<Source> sources)
    {
        this.days = days;
        this.total = total;
        this.trend = List.copyOf(trend);
        this.sources = List.copyOf(sources);
    }

    @Value
    public static class Day
    {
        private String date;
        private long count;
    }

    @Value
    public static class Source
    {
        private String name;
        private long count;
    }
}
