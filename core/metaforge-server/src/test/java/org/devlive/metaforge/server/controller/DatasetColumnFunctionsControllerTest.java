package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.enums.FunctionType;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.FunctionEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.initializer.DataSetConfigure;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.service.DataSetService;
import org.devlive.metaforge.service.service.FunctionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DatasetColumnFunctionsControllerTest
{
    private final FunctionService functions = mock(FunctionService.class);
    private final InitializerConfigure initializer = mock(InitializerConfigure.class);
    private final DataSetService datasets = mock(DataSetService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DataSetController(mock(DataSetRepository.class), datasets, functions, initializer)).build();

    @Test
    public void catalogComesFromTheDatasetTargetEngine()
            throws Exception
    {
        DataSetConfigure target = mock(DataSetConfigure.class);
        when(initializer.getDataSetConfigure()).thenReturn(target);
        when(target.getType()).thenReturn("ClickHouse");
        FunctionEntity function = new FunctionEntity();
        function.setName("round");
        function.setType(FunctionType.FUNCTION);
        FunctionEntity keyword = new FunctionEntity();
        keyword.setName("SELECT");
        keyword.setType(FunctionType.KEYWORD);
        when(functions.getAllByPlugin("ClickHouse")).thenReturn(CommonResponse.success(PageEntity.build(new PageImpl<>(List.of(function, keyword)))));
        mvc.perform(get("/api/v1/dataset/columnFunctions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.engine").value("ClickHouse"))
                .andExpect(jsonPath("$.data.functions.length()").value(1))
                .andExpect(jsonPath("$.data.functions[0].name").value("round"));
    }

    @Test
    public void validationIsRealAndRejectsUnknownColumns()
            throws Exception
    {
        String columns = "[{\"name\":\"amount\"},{\"name\":\"net\",\"virtualColumn\":true,\"formula\":\"round(amount, 2)\"}]";
        mvc.perform(post("/api/v1/dataset/validateColumns").contentType(MediaType.APPLICATION_JSON).content(columns))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true));
        mvc.perform(post("/api/v1/dataset/validateColumns").contentType(MediaType.APPLICATION_JSON).content(columns.replace("round(amount, 2)", "round(unknown, 2)")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(false));
    }
    @Test
    public void storedPreviewUsesTheDatasetService()
            throws Exception
    {
        when(datasets.preview("TARGET")).thenReturn(CommonResponse.success(org.devlive.metaforge.spi.model.Response.builder().isSuccessful(true).build()));
        mvc.perform(get("/api/v1/dataset/preview/TARGET")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true));
        org.mockito.Mockito.verify(datasets).preview("TARGET");
    }

    @Test
    public void newDatasetWithoutAClientCodeReceivesAServerGeneratedIdentity()
            throws Exception
    {
        org.mockito.Mockito.when(datasets.saveOrUpdate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> CommonResponse.success(invocation.getArgument(1)));
        mvc.perform(post("/api/v1/dataset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New dataset\",\"source\":{\"code\":\"A\"},\"columns\":[{\"name\":\"id\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.code").isNotEmpty());
    }

}
