package org.devlive.metaforge.server.controller.admin;

import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.server.controller.BaseController;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.record.TreeRecord;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/v1/role")
public class RoleController
        extends BaseController<RoleEntity>
{
    private final RoleRepository repository;
    private final RoleService service;

    public RoleController(RoleRepository repository, RoleService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }

    @GetMapping("check-code")
    public CommonResponse<Boolean> checkCode(@RequestParam("code") String code)
    {
        return service.checkCode(code);
    }

    @Override
    @PostMapping
    @JsonView(EntityView.UserView.class)
    public CommonResponse<RoleEntity> create(@RequestBody RoleEntity configure)
    {
        return service.createRole(configure);
    }

    @PutMapping("{code}")
    @JsonView(EntityView.UserView.class)
    public CommonResponse<RoleEntity> updateByCode(@PathVariable("code") String code, @RequestBody RoleEntity configure)
    {
        return service.updateRole(code, configure);
    }

    @Override
    @PutMapping
    @JsonView(EntityView.UserView.class)
    public CommonResponse<RoleEntity> update(@RequestBody RoleEntity configure)
    {
        if (configure.getId() != null) {
            return repository.findById(configure.getId())
                    .map(entity -> service.updateRole(entity.getCode(), configure))
                    .orElseGet(() -> CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND));
        }
        if (StringUtils.isBlank(configure.getCode())) {
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_INVALID);
        }
        return service.updateRole(configure.getCode(), configure);
    }

    @GetMapping("{id}/menus")
    public CommonResponse<List<TreeRecord>> getMenusByRoleId(@PathVariable("id") Long id)
    {
        return service.getMenusByRoleId(id);
    }

    @PutMapping("{id}/menus")
    public CommonResponse<Boolean> saveMenusByRoleId(@PathVariable("id") Long id, @RequestBody Set<Long> nodes)
    {
        return service.saveMenusByRoleId(id, nodes);
    }
}
