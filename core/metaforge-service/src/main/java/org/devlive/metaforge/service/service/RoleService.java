package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.record.TreeRecord;

import java.util.List;
import java.util.Set;

public interface RoleService
        extends BaseService<RoleEntity>
{
    CommonResponse<Boolean> checkCode(String code);

    CommonResponse<RoleEntity> createRole(RoleEntity configure);

    CommonResponse<RoleEntity> updateRole(String originalCode, RoleEntity configure);

    CommonResponse<List<TreeRecord>> getMenusByRoleId(Long roleId);

    CommonResponse<Boolean> saveMenusByRoleId(Long roleId, Set<Long> menus);
}
