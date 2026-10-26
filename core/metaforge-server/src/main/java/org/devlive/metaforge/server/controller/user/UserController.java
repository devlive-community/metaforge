package org.devlive.metaforge.server.controller.user;

import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.server.controller.BaseController;
import org.devlive.metaforge.service.annotation.DynamicJsonView;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.UploadBody;
import org.devlive.metaforge.service.body.UserNameBody;
import org.devlive.metaforge.service.body.UserPasswordBody;
import org.devlive.metaforge.service.body.user.UserRole;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.UserLogEntity;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.entity.itransient.user.UserEditorEntity;
import org.devlive.metaforge.service.model.AiModel;
import org.devlive.metaforge.service.record.TreeRecord;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.service.UserLogService;
import org.devlive.metaforge.service.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/user")
public class UserController
        extends BaseController<UserEntity>
{
    private final UserRepository repository;
    private final UserService service;
    private final UserLogService userLogService;
    private final RoleRepository roleRepository;

    public UserController(UserRepository repository, UserService service, UserLogService userLogService, RoleRepository roleRepository)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
        this.userLogService = userLogService;
        this.roleRepository = roleRepository;
    }

    @GetMapping(value = {"{code}", ""})
    @DynamicJsonView
    public CommonResponse<UserEntity> info(@PathVariable(required = false) String code)
    {
        return this.service.info(code);
    }

    @PutMapping(value = "changePassword")
    public CommonResponse<Long> changePassword(@Validated @RequestBody UserPasswordBody configure)
    {
        return this.service.changePassword(configure);
    }

    @GetMapping(value = "checkUsername", params = "username")
    public CommonResponse<Boolean> checkUsername(@RequestParam(value = "username") String username)
    {
        return service.checkUsername(username);
    }

    @PutMapping(value = "changeUsername")
    public CommonResponse<Long> changeUsername(@Validated @RequestBody UserNameBody configure)
    {
        return this.service.changeUsername(configure);
    }

    @PostMapping(value = "testChat")
    public CommonResponse<Boolean> testChat(@RequestBody AiModel configure)
    {
        return service.testChat(configure);
    }

    @PutMapping(value = "changeThirdConfigure")
    public CommonResponse<Long> changeThirdConfigure(@Validated @RequestBody AiModel configure)
    {
        return this.service.changeThirdConfigure(configure);
    }

    @PostMapping(value = "log")
    public CommonResponse<PageEntity<UserLogEntity>> getAllLogByFilter(@RequestBody FilterBody filter)
    {
        return this.userLogService.getAllByFilter(filter);
    }

    @GetMapping(value = "menus")
    @DynamicJsonView
    public CommonResponse<List<TreeRecord>> getMenus()
    {
        return this.service.getMenus();
    }

    @PutMapping(value = "allocationRole")
    public CommonResponse<UserEntity> allocationRole(@RequestBody UserRole configure)
    {
        UserEntity user = new UserEntity();
        user.setId(configure.getUserId());
        Set<RoleEntity> roles = Sets.newHashSet();
        configure.getRoles()
                .forEach(code -> roleRepository.findByCode(code)
                        .ifPresent(roles::add));
        user.setRoles(roles);
        return this.service.saveOrUpdate(user);
    }

    @PutMapping(value = "changeEditorConfigure")
    public CommonResponse<UserEntity> changeEditorConfigure(@Validated @RequestBody UserEditorEntity configure)
    {
        return this.service.changeEditorConfigure(configure);
    }

    @SneakyThrows
    @PostMapping("uploadAvatar")
    public CommonResponse<AvatarEntity> uploadAvatar(@ModelAttribute UploadBody configure)
    {
        return this.service.uploadAvatar(configure);
    }

    @PutMapping(value = "change-notify")
    public CommonResponse<UserEntity> changeNotify(@RequestBody UserEntity configure)
    {
        return this.service.changeNotify(configure);
    }
}
