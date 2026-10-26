package org.devlive.metaforge.server.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.CodeUtils;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.annotation.DynamicJsonView;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.BaseEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.service.BaseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;

public abstract class BaseController<T extends BaseEntity>
        implements Serializable
{
    private final BaseRepository<T, Long> repository;
    private final BaseService<T> service;

    protected BaseController(BaseRepository<T, Long> repository, BaseService<T> service)
    {
        this.repository = repository;
        this.service = service;
    }

    /**
     * Get data based on pagination
     */
    @PostMapping(value = "list")
    @DynamicJsonView
    public CommonResponse<PageEntity<T>> list(@RequestBody FilterBody filter)
    {
        return service.getAll(repository, filter);
    }

    /**
     * Create new resource
     */
    @PostMapping
    @JsonView(value = {EntityView.UserView.class})
    public CommonResponse<T> create(@RequestBody T configure)
    {
        configure.setCode(CodeUtils.generateCode(false));
        return service.saveOrUpdate(repository, configure);
    }

    /**
     * Update existing resource
     */
    @PutMapping
    @JsonView(value = {EntityView.UserView.class})
    public CommonResponse<T> update(@RequestBody T configure)
    {
        return repository.findByCode(configure.getCode())
                .map(entity -> {
                    configure.setId(entity.getId());
                    configure.setCode(entity.getCode());
                    return service.saveOrUpdate(repository, configure);
                })
                .orElseGet(() -> CommonResponse.failure("Resource [ " + configure.getCode() + " ] not found"));
    }

    @Deprecated
    @DeleteMapping
    public CommonResponse<String> delete(@RequestParam(value = "code") String code)
    {
        return service.deleteByCode(repository, code);
    }

    @DeleteMapping(value = "{code}")
    public CommonResponse<String> deleteForPath(@PathVariable(value = "code") String code)
    {
        return service.deleteByCode(repository, code);
    }

    /**
     * Retrieves information for a specific path.
     *
     * @param id the identifier of the path
     * @return the information for the specified path
     */
    @GetMapping(value = "{id}")
    public CommonResponse<T> getInfoForPath(@PathVariable(value = "id") Long id)
    {
        return service.getById(repository, id);
    }

    @GetMapping(value = "info/{code}")
    @JsonView(value = {EntityView.UserView.class})
    public CommonResponse<T> getByCode(@PathVariable(value = "code") String code)
    {
        return service.getByCode(repository, code);
    }
}
