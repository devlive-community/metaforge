package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.CodeUtils;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.initializer.RoleCodeConstraint;
import org.devlive.metaforge.service.record.TreeRecord;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.repository.jdbc.MenuAccessRepository;
import org.devlive.metaforge.service.service.RoleService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class RoleServiceImpl
        implements RoleService
{
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$");

    private final RoleRepository repository;
    private final MenuAccessRepository menuRepository;
    private final RoleCodeConstraint codeConstraint;
    private final LinkRepository linkRepository;

    public RoleServiceImpl(RoleRepository repository, MenuAccessRepository menuRepository, RoleCodeConstraint codeConstraint, LinkRepository linkRepository)
    {
        this.linkRepository = linkRepository;
        this.repository = repository;
        this.menuRepository = menuRepository;
        this.codeConstraint = codeConstraint;
    }

    @Override
    public CommonResponse<Boolean> checkCode(String code)
    {
        String normalized = StringUtils.trimToNull(code);
        if (normalized == null) {
            return CommonResponse.success(true);
        }
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_INVALID);
        }
        return CommonResponse.success(!repository.existsByCode(normalized));
    }

    @Override
    public CommonResponse<RoleEntity> createRole(RoleEntity configure)
    {
        if (configure.getId() != null) {
            return CommonResponse.failure(ServiceState.USER_ROLE_ID_MISMATCH);
        }
        String code = StringUtils.trimToNull(configure.getCode());
        if (code == null) {
            code = CodeUtils.generateCode(false);
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_INVALID);
        }
        if (repository.existsByCode(code)) {
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_EXISTS);
        }
        try {
            codeConstraint.ensure();
            RoleEntity entity = new RoleEntity();
            entity.setCode(code);
            entity.setName(configure.getName());
            entity.setDescription(configure.getDescription());
            return CommonResponse.success(repository.save(entity));
        }
        catch (DataIntegrityViolationException exception) {
            if (repository.existsByCode(code)) {
                return CommonResponse.failure(ServiceState.USER_ROLE_CODE_EXISTS);
            }
            log.error("Could not create role", exception);
            return CommonResponse.failure("Could not create role");
        }
        catch (SQLException exception) {
            log.error("Could not enforce unique role codes", exception);
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_CONSTRAINT_FAILED);
        }
    }

    @Override
    public CommonResponse<RoleEntity> updateRole(String originalCode, RoleEntity configure)
    {
        return repository.findByCode(originalCode)
                .map(entity -> {
                    if (configure.getId() != null && !Objects.equals(configure.getId(), entity.getId())) {
                        return CommonResponse.<RoleEntity>failure(ServiceState.USER_ROLE_ID_MISMATCH);
                    }
                    if (configure.getCode() != null && !Objects.equals(configure.getCode(), entity.getCode())) {
                        return CommonResponse.<RoleEntity>failure(ServiceState.USER_ROLE_CODE_IMMUTABLE);
                    }
                    if (configure.getName() != null) {
                        entity.setName(configure.getName());
                    }
                    if (configure.getDescription() != null) {
                        entity.setDescription(configure.getDescription());
                    }
                    return CommonResponse.success(repository.save(entity));
                })
                .orElseGet(() -> CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND));
    }

    @Override
    public CommonResponse<List<TreeRecord>> getMenusByRoleId(Long roleId)
    {
        try {
            return menuRepository.findRoleMenus(roleId).map(CommonResponse::success)
                    .orElseGet(() -> CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND));
        }
        catch (DataAccessException exception) {
            log.error("Could not load role menus", exception);
            return CommonResponse.failure(ServiceState.USER_ROLE_MENU_READ_FAILED);
        }
    }

    @Override
    public CommonResponse<Boolean> saveMenusByRoleId(Long roleId, Set<Long> menus)
    {
        try {
            MenuAccessRepository.SaveResult result = menuRepository.replaceRoleMenus(roleId, menus);
            if (result == MenuAccessRepository.SaveResult.ROLE_NOT_FOUND) {
                return CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND);
            }
            if (result == MenuAccessRepository.SaveResult.INVALID_MENUS) {
                return CommonResponse.failure(ServiceState.USER_ROLE_MENU_INVALID);
            }
            return CommonResponse.success(true);
        }
        catch (DataAccessException exception) {
            log.error("Could not save role menus", exception);
            return CommonResponse.failure(ServiceState.USER_ROLE_MENU_SAVE_FAILED);
        }
    }

    @Override
    @Transactional
    public CommonResponse<String> deleteByCode(BaseRepository<RoleEntity, Long> repository, String code)
    {
        repository.findByCode(code).ifPresent(value -> linkRepository.deleteRole(value.getId()));
        return RoleService.super.deleteByCode(repository, code);
    }
}
