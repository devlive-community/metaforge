package org.devlive.metaforge.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickHouseIndexTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Answers the two system-table queries with fixed rows, recording the SQL it was asked. */
    private static final class FakeClickHouse
            extends ClickHouseService
    {
        private final Map<String, Object> keys;
        private final List<Map<String, Object>> skipping;
        private final List<String> queries = new ArrayList<>();

        private FakeClickHouse(Map<String, Object> keys, List<Map<String, Object>> skipping)
        {
            this.keys = keys;
            this.skipping = skipping;
        }

        @Override
        public Response execute(RuntimeConfigure configure, String content)
        {
            queries.add(content);
            Response response = new Response();
            if (content.contains("system.tables")) {
                response.setIsSuccessful(true);
                response.setColumns(new ArrayList<>(List.of(MAPPER.valueToTree(keys))));
            }
            else if (skipping == null) {
                response.setIsSuccessful(false);
                response.setMessage("Table system.data_skipping_indices does not exist");
            }
            else {
                response.setIsSuccessful(true);
                response.setColumns(skipping.stream().map(row -> (Object) MAPPER.valueToTree(row)).collect(Collectors.toList()));
            }
            return response;
        }
    }

    private static List<JsonNode> rows(Response response)
    {
        return response.getColumns().stream().map(JsonNode.class::cast).collect(Collectors.toList());
    }

    @Test
    public void primaryKeyAndSkippingIndexesComeFromClickHouseSystemTables()
    {
        FakeClickHouse service = new FakeClickHouse(Map.of("primary_key", "event_date, cityHash64(user_id, site)", "sorting_key", "event_date, cityHash64(user_id, site), id"),
                List.of(Map.of("name", "idx_url", "type", "bloom_filter", "expr", "url", "granularity", 4)));
        Response response = service.getIndexDetails(null, "analytics", "events");

        Assert.assertTrue(response.getIsSuccessful());
        Assert.assertEquals(ClickHouseService.INDEX_HEADERS, response.getHeaders());
        List<JsonNode> rows = rows(response);
        Assert.assertEquals(3, rows.size());
        Assert.assertEquals("PRIMARY", rows.get(0).path("index_name").asText());
        Assert.assertEquals("event_date", rows.get(0).path("column_name").asText());
        Assert.assertEquals("cityHash64(user_id, site)", rows.get(1).path("column_name").asText());
        Assert.assertEquals(2, rows.get(1).path("seq_in_index").asInt());
        Assert.assertEquals("a MergeTree primary key is not unique", 1, rows.get(0).path("non_unique").asInt());
        Assert.assertEquals("idx_url", rows.get(2).path("index_name").asText());
        Assert.assertEquals("BLOOM_FILTER", rows.get(2).path("index_type").asText());
        Assert.assertEquals("GRANULARITY 4", rows.get(2).path("index_comment").asText());
        // No MySQL information_schema SQL reaches ClickHouse.
        Assert.assertTrue(service.queries.stream().noneMatch(sql -> sql.contains("STATISTICS")));
        Assert.assertTrue(service.queries.get(0).contains("database = 'analytics' AND name = 'events'"));
    }

    @Test
    public void sortingKeyStandsInWithoutAPrimaryKeyAndOldServersStillListIt()
    {
        FakeClickHouse service = new FakeClickHouse(Map.of("primary_key", "", "sorting_key", "id"), null);
        List<JsonNode> rows = rows(service.getIndexDetails(null, "db", "t"));
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals("id", rows.get(0).path("column_name").asText());
    }

    @Test
    public void namesAreQuotedAsLiterals()
    {
        FakeClickHouse service = new FakeClickHouse(Map.of("primary_key", "", "sorting_key", ""), List.of());
        Response response = service.getIndexDetails(null, "db", "x' OR 1=1 --");
        Assert.assertTrue(response.getColumns().isEmpty());
        Assert.assertTrue(service.queries.get(0).contains("name = 'x'' OR 1=1 --'"));
    }

    @Test
    public void keyExpressionsSplitOnlyAtTopLevelCommas()
    {
        Assert.assertEquals(List.of("a", "toYYYYMM(d)", "tuple(x, y)", "arr[1]", "'a,b'"),
                ClickHouseService.splitKeyExpression("a, toYYYYMM(d), tuple(x, y), arr[1], 'a,b'"));
        Assert.assertEquals(List.of(), ClickHouseService.splitKeyExpression(""));
    }
}
