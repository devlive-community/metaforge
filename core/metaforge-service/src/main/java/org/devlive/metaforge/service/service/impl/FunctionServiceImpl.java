package org.devlive.metaforge.service.service.impl;

import org.apache.commons.io.IOUtils;
import org.devlive.metaforge.common.enums.FunctionImportMode;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.FunctionsImportBody;
import org.devlive.metaforge.service.entity.FunctionEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.repository.FunctionsRepository;
import org.devlive.metaforge.service.service.FunctionService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FunctionServiceImpl
        implements FunctionService
{
    private final FunctionsRepository functionsRepository;

    public FunctionServiceImpl(FunctionsRepository functionsRepository)
    {
        this.functionsRepository = functionsRepository;
    }

    @Override
    public CommonResponse<PageEntity<FunctionEntity>> getAllByFilter(FilterBody filter)
    {
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(this.functionsRepository.findAll(pageable)));
    }

    @Override
    public CommonResponse<FunctionEntity> getById(Long id)
    {
        return this.functionsRepository.findById(id)
                .map(CommonResponse::success)
                .orElse(CommonResponse.failure(String.format("Function [ %s ] not found", id)));
    }

    @Override
    public CommonResponse<Object> batchImport(FunctionsImportBody configure)
    {
        List<FunctionEntity> functions = new ArrayList<>();
        if (configure.getMode().equals(FunctionImportMode.txt)) {
            Arrays.stream(configure.getContent().split("\n")).forEach(value -> {
                FunctionEntity function = new FunctionEntity();
                function.setContent(value);
                function.setName(value);
                function.setExample(value);
                function.setPlugin(configure.getPlugin());
                function.setDescription(value);
                function.setType(configure.getType());
                functions.add(function);
            });
        }
        else {
            try {
                URI uri = new URI(configure.getContent());
                Arrays.stream(IOUtils.toString(uri).split("\n")).forEach(value -> {
                    FunctionEntity function = new FunctionEntity();
                    function.setContent(value);
                    function.setName(value);
                    function.setExample(value);
                    function.setPlugin(configure.getPlugin());
                    function.setDescription(value);
                    function.setType(configure.getType());
                    functions.add(function);
                });
            }
            catch (URISyntaxException | IOException e) {
                return CommonResponse.failure(ServiceState.INVALID_REMOTE_ADDRESS, ServiceState.INVALID_REMOTE_ADDRESS.getValue() + ":" + e.getMessage());
            }
        }
        return CommonResponse.success(this.functionsRepository.saveAll(functions));
    }

    @Override
    public CommonResponse<PageEntity<FunctionEntity>> getAllByPlugin(String plugin)
    {
        FilterBody filter = new FilterBody();
        filter.setSize(Integer.MAX_VALUE);
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(this.functionsRepository.findAllByPluginContaining(pageable, plugin)));
    }
}
