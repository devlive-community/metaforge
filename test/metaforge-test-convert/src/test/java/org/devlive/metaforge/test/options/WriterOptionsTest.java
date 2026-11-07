package org.devlive.metaforge.test.options;

import org.devlive.metaforge.convert.ConvertService;
import org.devlive.metaforge.convert.TxtConvertService;
import org.devlive.metaforge.convert.csv.CsvConvertService;
import org.devlive.metaforge.convert.json.JsonConvertService;
import org.devlive.metaforge.convert.model.ConvertOption;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.convert.xml.XmlConvertService;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Every writer setting a convert plugin declares changes the file it writes. */
public class WriterOptionsTest
{
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private String write(ConvertService service, Map<String, String> options)
            throws IOException
    {
        File directory = folder.newFolder();
        ConvertRequest request = new ConvertRequest();
        request.setName("t");
        request.setPath(directory.getAbsolutePath());
        request.setDelimiter("\t");
        request.setHeaders(List.of("id", "name", "at"));
        request.setColumns(List.of(Arrays.asList(1, "a,b", Timestamp.valueOf("2026-10-30 09:20:00")), Arrays.asList(2, null, null)));
        request.setOptions(options);
        ConvertResponse response = service.writer(request);
        Assert.assertTrue(response.getMessage(), Boolean.TRUE.equals(response.getSuccessful()));
        File[] files = directory.listFiles();
        Assert.assertNotNull(files);
        Assert.assertEquals(1, files.length);
        Assert.assertTrue(files[0].getName(), files[0].getName().endsWith("." + service.extension()));
        return Files.readString(files[0].toPath(), StandardCharsets.UTF_8);
    }

    private static List<String> names(ConvertService service)
    {
        return service.options().stream().map(ConvertOption::getName).collect(Collectors.toList());
    }

    @Test
    public void jsonWritesArraysOrLinesWithTheChosenIndentDatesAndNulls()
            throws IOException
    {
        JsonConvertService json = new JsonConvertService();
        Assert.assertEquals(List.of("root", "indent", "dateFormat", "nulls"), names(json));
        String pretty = write(json, Map.of("indent", "2", "dateFormat", "datetime"));
        Assert.assertTrue(pretty, pretty.startsWith("[ {\n  \"id\" : 1,") || pretty.startsWith("[\n  {"));
        Assert.assertTrue(pretty, pretty.contains("\"2026-10-30 09:20:00\""));
        Assert.assertTrue(pretty, pretty.contains("\"name\" : null"));
        String lines = write(json, Map.of("root", "lines", "nulls", "omit", "dateFormat", "epoch"));
        String[] rows = lines.trim().split("\n");
        Assert.assertEquals(lines, 2, rows.length);
        Assert.assertTrue(rows[0], rows[0].startsWith("{\"id\":1,\"name\":\"a,b\",\"at\":") && !rows[0].contains("2026"));
        Assert.assertEquals("{\"id\":2}", rows[1]);
    }

    @Test
    public void csvQuotesCellsAndHonoursDelimiterHeaderAndNulls()
            throws IOException
    {
        CsvConvertService csv = new CsvConvertService();
        Assert.assertEquals(List.of("delimiter", "header", "nulls"), names(csv));
        Assert.assertEquals("id,name,at\n1,\"a,b\",2026-10-30 09:20:00.0\n2,,", write(csv, Map.of()));
        Assert.assertEquals("1;a,b;2026-10-30 09:20:00.0\n2;NULL;NULL", write(csv, Map.of("delimiter", ";", "header", "false", "nulls", "NULL")));
    }

    @Test
    public void txtTakesItsDelimiterAndHeaderFromTheOptions()
            throws IOException
    {
        TxtConvertService txt = new TxtConvertService();
        Assert.assertEquals("1|a,b|2026-10-30 09:20:00.0\n2||", write(txt, Map.of("delimiter", "|", "header", "false")).replaceFirst("^\n", ""));
    }

    @Test
    public void xmlNamesItsElementsAndCanLeaveNullsOut()
            throws IOException
    {
        String xml = write(new XmlConvertService(), Map.of("rootElement", "rows", "rowElement", "row", "nulls", "omit"));
        Assert.assertTrue(xml, xml.startsWith("<rows><row id=\"1\""));
        Assert.assertTrue(xml, xml.contains("<row id=\"2\"/>") || xml.contains("<row id=\"2\"></row>"));
    }
}
