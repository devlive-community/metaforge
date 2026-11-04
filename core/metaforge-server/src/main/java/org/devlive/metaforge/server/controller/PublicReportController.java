package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.ReportShareService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anonymous access to shared reports by their share token. Only the saved report definition is
 * evaluated; visitors cannot send SQL, queries or report codes.
 */
@RestController
@RequestMapping(value = "/api/v1/public/report")
public class PublicReportController
{
    private final ReportShareService service;

    public PublicReportController(ReportShareService service)
    {
        this.service = service;
    }

    @GetMapping("{token}")
    public ResponseEntity<CommonResponse<ReportShareService.PublicReport>> view(@PathVariable("token") String token)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.view(token));
    }

    @GetMapping("{token}/refresh")
    public ResponseEntity<CommonResponse<List<Map<String, Object>>>> refresh(@PathVariable("token") String token)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30))).body(service.refresh(token));
    }

    @GetMapping("{token}/table")
    public ResponseEntity<CommonResponse<ReportShareService.Table>> table(@PathVariable("token") String token)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30))).body(service.table(token));
    }

    @GetMapping("{token}/download")
    public ResponseEntity<byte[]> download(@PathVariable("token") String token)
    {
        return service.download(token)
                .map(entry -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename(entry.getKey() + ".csv", StandardCharsets.UTF_8).build().toString())
                        .body(csv(entry.getValue())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** UTF-8 CSV with a byte order mark for spreadsheet apps; cells that would start a formula are quoted as text. */
    static byte[] csv(ReportShareService.Table table)
    {
        StringBuilder out = new StringBuilder("\uFEFF");
        line(out, new ArrayList<>(table.getHeaders()));
        table.getRows().forEach(row -> line(out, row));
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder out, List<?> cells)
    {
        for (int index = 0; index < cells.size(); index++) {
            if (index > 0) {
                out.append(',');
            }
            Object value = cells.get(index);
            String cell = value == null ? "" : String.valueOf(value);
            if (!(value instanceof Number) && !cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) {
                cell = "'" + cell;
            }
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
                cell = "\"" + cell.replace("\"", "\"\"") + "\"";
            }
            out.append(cell);
        }
        out.append("\r\n");
    }

    @GetMapping("{token}/data")
    public ResponseEntity<CommonResponse<Object>> data(@PathVariable("token") String token)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30))).body(service.data(token));
    }
}
