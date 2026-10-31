package org.devlive.metaforge.server.controller;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.converter.DynamicJsonViewHttpMessageConverter;
import org.devlive.metaforge.service.service.MetadataService;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementEntry;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SourceManagementControllerTest
{
    private final MetadataService service = mock(MetadataService.class);
    private final ObjectMapper mapper = new ObjectMapper().disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SourceManagementController(service), new MetadataController(service))
            .setControllerAdvice(new DynamicJsonViewHttpMessageConverter(mapper))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();

    @Test
    public void capabilitiesAreSerializedForUsersAndNeverMatchTheDatabaseRoute()
            throws Exception
    {
        when(service.getCapabilities("source-code")).thenReturn(CommonResponse.success(ManagementCapabilities.forJdbc("mysql")));
        mvc.perform(get("/api/v1/source-management/source-code/capabilities").requestAttr("JSON_VIEW", EntityView.UserView.class))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.kind").value("relational"))
                .andExpect(jsonPath("$.data.capabilities", hasItem("column:modify")));
        verify(service, never()).getDatabase("source-code", "capabilities");
    }

    @Test
    public void nativePageFlagsAndEntriesSurviveTheActiveJsonView()
            throws Exception
    {
        ManagementEntry entry = new ManagementEntry();
        entry.setId("k:aGVsbG8");
        entry.setName("hello");
        entry.setType("string");
        entry.setTtl(1000L);
        ManagementPage page = new ManagementPage();
        page.getEntries().add(entry);
        page.setHasMore(true);
        page.setNextCursor("42");
        when(service.managementList(eq("source-code"), any())).thenReturn(CommonResponse.success(page));
        mvc.perform(post("/api/v1/source-management/source-code/browse").requestAttr("JSON_VIEW", EntityView.UserView.class)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"namespace\":\"0\",\"limit\":50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("42"))
                .andExpect(jsonPath("$.data.entries[0].name").value("hello"))
                .andExpect(jsonPath("$.data.entries[0].ttl").value(1000));
    }

    @Test
    public void aSingleBatchEndpointReturnsEverySqlTabUnderTheUserJsonView()
            throws Exception
    {
        when(service.previewCreateTableBatch(eq("source-code"), any())).thenReturn(CommonResponse.success(
                Map.of("full", "CREATE TABLE users;", "columns", "column SQL", "indexes", "index SQL", "options", "option SQL")));
        mvc.perform(post("/api/v1/metadata/source-code/preview-create-table-batch").requestAttr("JSON_VIEW", EntityView.UserView.class)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"database\":\"app\",\"name\":\"users\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.full").value("CREATE TABLE users;"))
                .andExpect(jsonPath("$.data.columns").value("column SQL"))
                .andExpect(jsonPath("$.data.indexes").value("index SQL"))
                .andExpect(jsonPath("$.data.options").value("option SQL"));
        verify(service, never()).previewCreateTable(any(), any(), any());
    }
}
