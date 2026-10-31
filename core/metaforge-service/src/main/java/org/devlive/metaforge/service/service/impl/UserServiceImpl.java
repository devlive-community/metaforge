package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.response.JwtResponse;
import org.devlive.metaforge.common.utils.CodeUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.common.utils.NullAwareBeanUtils;
import org.devlive.metaforge.common.utils.UrlUtils;
import org.devlive.metaforge.fs.FsRequest;
import org.devlive.metaforge.fs.FsResponse;
import org.devlive.metaforge.fs.FsService;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.audit.AuditUserLog;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.UploadBody;
import org.devlive.metaforge.service.body.UserNameBody;
import org.devlive.metaforge.service.body.UserPasswordBody;
import org.devlive.metaforge.service.body.UserResetPasswordBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.entity.itransient.user.UserEditorEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.model.AiModel;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.record.TreeRecord;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.NotificationRepository;
import org.devlive.metaforge.service.repository.PluginAuditRepository;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.ScheduledHistoryRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.repository.jdbc.MenuAccessRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.JwtService;
import org.devlive.metaforge.service.service.UserService;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "EI_EXPOSE_REP2"})
@Service
@Slf4j
public class UserServiceImpl
        implements UserService
{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SourceRepository sourceRepository;
    private final MenuAccessRepository menuRepository;
    private final LinkRepository linkRepository;
    private final NotificationRepository notificationRepository;
    private final PluginAuditRepository pluginAuditRepository;
    private final ScheduledHistoryRepository scheduledHistoryRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisTemplate redisTemplate;
    private final Environment environment;
    private final InitializerConfigure initializer;
    private final PluginManager pluginManager;
    private final HttpServletRequest request;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, SourceRepository sourceRepository, MenuAccessRepository menuRepository, LinkRepository linkRepository, NotificationRepository notificationRepository, PluginAuditRepository pluginAuditRepository, ScheduledHistoryRepository scheduledHistoryRepository, PasswordEncoder encoder, AuthenticationManager authenticationManager, JwtService jwtService, RedisTemplate redisTemplate, Environment environment, InitializerConfigure initializer, PluginManager pluginManager, HttpServletRequest request)
    {
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
        this.notificationRepository = notificationRepository;
        this.pluginAuditRepository = pluginAuditRepository;
        this.scheduledHistoryRepository = scheduledHistoryRepository;
        this.roleRepository = roleRepository;
        this.sourceRepository = sourceRepository;
        this.menuRepository = menuRepository;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.initializer = initializer;
        this.pluginManager = pluginManager;
        this.request = request;
    }

    @Override
    @Transactional
    public CommonResponse<UserEntity> saveOrUpdate(UserEntity configure)
    {
        // null roles keep the current assignment; a set (even empty) replaces it
        Set<Long> roleIds = configure.getRoles() == null ? null : resolveRoleIds(configure.getRoles());
        if (ObjectUtils.isEmpty(configure.getId())) {
            Optional<UserEntity> userOptional = this.userRepository.findByUsername(configure.getUsername());
            if (userOptional.isPresent()) {
                return CommonResponse.failure(ServiceState.USER_EXISTS);
            }
            configure.setCode(CodeUtils.generateCode(false));
            configure.setUsername(configure.getUsername());
            configure.setPassword(encoder.encode(configure.getPassword()));
            if (ObjectUtils.isEmpty(roleIds)) {
                Optional<RoleEntity> userRoleOptional = roleRepository.findByCode("USER").or(() -> roleRepository.findByName("User"));
                if (userRoleOptional.isEmpty()) {
                    return CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND);
                }
                roleIds = Set.of(userRoleOptional.get().getId());
            }
        }
        else {
            userRepository.findById(configure.getId())
                    .ifPresent(value -> {
                        NullAwareBeanUtils.copyNullProperties(value, configure);
                        if (StringUtils.isEmpty(value.getCode())) {
                            value.setCode(CodeUtils.generateCode(false));
                        }
                    });
        }
        UserEntity saved = userRepository.save(configure);
        if (roleIds != null) {
            linkRepository.replaceUserRoles(saved.getId(), roleIds);
        }
        return CommonResponse.success(saved);
    }

    private Set<Long> resolveRoleIds(Set<RoleEntity> roles)
    {
        Set<Long> ids = new HashSet<>();
        for (RoleEntity role : roles) {
            if (role.getId() != null) {
                ids.add(role.getId());
            }
            else if (role.getCode() != null) {
                roleRepository.findByCode(role.getCode()).ifPresent(found -> ids.add(found.getId()));
            }
        }
        return ids;
    }

    @Override
    @Transactional
    public CommonResponse<String> deleteByCode(BaseRepository<UserEntity, Long> repository, String code)
    {
        return userRepository.findByCode(code)
                .map(user -> {
                    if (user.isSystem()) {
                        return CommonResponse.<String>failure("The built-in account cannot be deleted");
                    }
                    if (user.getId().equals(UserDetailsService.getUser().getId())) {
                        return CommonResponse.<String>failure("The currently signed-in account cannot be deleted");
                    }
                    // owned rows that JPA used to cascade
                    for (SourceEntity source : sourceRepository.findAllByUserId(user.getId())) {
                        pluginAuditRepository.deleteAllBySourceId(source.getId());
                        scheduledHistoryRepository.deleteAllBySourceId(source.getId());
                        sourceRepository.delete(source);
                    }
                    linkRepository.deleteUser(user.getId());
                    notificationRepository.deleteAllByUserId(user.getId());
                    userRepository.delete(user);
                    return CommonResponse.success(code);
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<UserEntity> saveOrUpdate(BaseRepository<UserEntity, Long> repository, UserEntity configure)
    {
        // edits without a new password keep the stored hash; the one-arg overload copies it back
        if (configure.getId() != null && StringUtils.isNotEmpty(configure.getPassword())) {
            configure.setPassword(encoder.encode(configure.getPassword()));
        }
        return this.saveOrUpdate(configure);
    }

    @AuditUserLog
    @Override
    public CommonResponse<Object> authenticate(UserEntity configure)
    {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(configure.getUsername(), configure.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.generateJwtToken(authentication);

        UserDetailsService userDetails = (UserDetailsService) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return CommonResponse.success(new JwtResponse(jwt, userDetails.getCode(), userDetails.getUsername(), roles, userDetails.getAvatar()));
    }

    @Override
    public CommonResponse<PageEntity<UserEntity>> getAll(BaseRepository<UserEntity, Long> repository, FilterBody filter)
    {
        CommonResponse<PageEntity<UserEntity>> response = UserService.super.getAll(repository, filter);
        if (response.getStatus() && response.getData() != null) {
            withRoles(response.getData().getContent());
        }
        return response;
    }

    @Override
    public CommonResponse<UserEntity> changeActive(BaseRepository<UserEntity, Long> repository, String code, boolean active)
    {
        if (!active) {
            Optional<UserEntity> userOptional = userRepository.findByCode(code);
            if (userOptional.isPresent()) {
                UserEntity user = userOptional.get();
                if (user.isSystem()) {
                    return CommonResponse.failure("The built-in account cannot be disabled");
                }
                if (user.getId().equals(UserDetailsService.getUser().getId())) {
                    return CommonResponse.failure("The currently signed-in account cannot be disabled");
                }
            }
        }
        return UserService.super.changeActive(repository, code, active);
    }

    @Override
    public CommonResponse<Long> resetPassword(String code, UserResetPasswordBody configure)
    {
        return userRepository.findByCode(code)
                .map(user -> {
                    user.setPassword(encoder.encode(configure.getPassword()));
                    userRepository.save(user);
                    return CommonResponse.success(user.getId());
                })
                .orElseGet(() -> CommonResponse.failure(ServiceState.USER_NOT_FOUND));
    }

    @Override
    public CommonResponse<UserEntity> info(String code)
    {
        return userRepository.findByCode(UserDetailsService.getUser().getCode())
                .map(user -> {
                    withRoles(Collections.singletonList(user));
                    user.setUnreadCount(notificationRepository.countByUserIdAndIsReadFalse(user.getId()));
                    return CommonResponse.success(user);
                })
                .orElseGet(() -> CommonResponse.failure(ServiceState.USER_NOT_FOUND));
    }

    @Override
    public CommonResponse<Long> changePassword(UserPasswordBody configure)
    {
        Optional<UserEntity> userOptional = this.userRepository.findById(UserDetailsService.getUser().getId());
        if (!userOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NOT_FOUND);
        }
        UserEntity user = userOptional.get();
        if (!encoder.matches(configure.getOldPassword(), user.getPassword())) {
            return CommonResponse.failure(ServiceState.USER_PASSWORD_INCORRECT);
        }
        if (!configure.getNewPassword().equals(configure.getConfirmPassword())) {
            return CommonResponse.failure(ServiceState.USER_PASSWORD_DIFFERENT);
        }
        user.setPassword(encoder.encode(configure.getNewPassword()));
        this.userRepository.save(user);
        return CommonResponse.success(user.getId());
    }

    @Override
    public CommonResponse<Boolean> checkUsername(String username)
    {
        if (!username.matches("^[A-Za-z_][A-Za-z0-9_]{3,19}$")) {
            return CommonResponse.failure(ServiceState.USER_NAME_FORMAT_INVALID);
        }
        if (this.userRepository.findByUsername(username).isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NAME_ALREADY_EXISTS);
        }
        return CommonResponse.success(true);
    }

    @Override
    public CommonResponse<Boolean> testChat(AiModel configure)
    {
        String host = StringUtils.stripEnd(configure.getHost(), "/");
        String url = host + "/models";
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) configure.getTimeout() * 1000);
            factory.setReadTimeout((int) configure.getTimeout() * 1000);
            RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + configure.getToken());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return CommonResponse.success(response.getStatusCode().is2xxSuccessful());
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "connection failed";
            }
            return CommonResponse.failure(String.format("Connection failed: %s", StringUtils.abbreviate(message, 200)));
        }
    }

    @Override
    public CommonResponse<Long> changeUsername(UserNameBody configure)
    {
        Optional<UserEntity> userOptional = this.userRepository.findById(UserDetailsService.getUser().getId());
        if (!userOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NOT_FOUND);
        }
        UserEntity user = userOptional.get();
        if (!encoder.matches(configure.getPassword(), user.getPassword())) {
            return CommonResponse.failure(ServiceState.USER_PASSWORD_INCORRECT);
        }
        if (configure.getNewUsername().equals(user.getUsername())) {
            return CommonResponse.failure(ServiceState.USER_NAME_EQUALS);
        }
        user.setUsername(configure.getNewUsername());
        this.userRepository.save(user);
        return CommonResponse.success(user.getId());
    }

    @Override
    public CommonResponse<Long> changeThirdConfigure(AiModel configure)
    {
        Optional<UserEntity> userOptional = this.userRepository.findById(UserDetailsService.getUser().getId());
        if (!userOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NOT_FOUND);
        }
        UserEntity user = userOptional.get();
        user.setChatConfigure(JsonUtils.toJSON(configure));
        this.userRepository.save(user);
        return CommonResponse.success(user.getId());
    }

    @Override
    public CommonResponse<List<Object>> getSugs(Long id)
    {
        Optional<SourceEntity> sourceEntityOptional = sourceRepository.findById(id);
        if (!sourceEntityOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.SOURCE_NOT_FOUND);
        }

        SourceEntity entity = sourceEntityOptional.get();
        String key = String.join("_", entity.getType(), entity.getId().toString());
        int sugsMaxSize = Integer.parseInt(environment.getProperty("metaforge.editor.sugs.maxSize"));
        List<Object> sugs = (List<Object>) redisTemplate.opsForSet()
                .members(key)
                .stream()
                .limit(sugsMaxSize)
                .collect(Collectors.toList());
        return CommonResponse.success(sugs);
    }

    @Override
    public CommonResponse<List<TreeRecord>> getMenus()
    {
        try {
            return menuRepository.findUserMenus(UserDetailsService.getUser().getCode()).map(CommonResponse::success)
                    .orElseGet(() -> CommonResponse.failure(ServiceState.USER_NOT_FOUND));
        }
        catch (DataAccessException exception) {
            log.error("Could not load user menus", exception);
            return CommonResponse.failure(ServiceState.USER_ROLE_MENU_READ_FAILED);
        }
    }

    @Override
    public CommonResponse<PageEntity<UserEntity>> getAll(FilterBody filter)
    {
        Page<UserEntity> page = Persistence.filtered(this.userRepository, filter, UserDetailsService.getUser());
        withRoles(page.getContent());
        return CommonResponse.success(PageEntity.build(page));
    }

    private void withRoles(List<UserEntity> users)
    {
        Map<Long, RoleEntity> roles = new HashMap<>();
        roleRepository.findAll().forEach(role -> roles.put(role.getId(), role));
        for (UserEntity user : users) {
            Set<RoleEntity> assigned = new HashSet<>();
            for (Long roleId : linkRepository.roleIdsOfUser(user.getId())) {
                Optional.ofNullable(roles.get(roleId)).ifPresent(assigned::add);
            }
            user.setRoles(assigned);
        }
    }

    @Override
    public CommonResponse<UserEntity> changeEditorConfigure(UserEditorEntity configure)
    {
        return userRepository.findByCode(UserDetailsService.getUser().getCode())
                .map(value -> {
                    value.setEditorConfigure(configure);
                    return CommonResponse.success(userRepository.save(value));
                })
                .orElseGet(() -> CommonResponse.failure(ServiceState.USER_NOT_FOUND));
    }

    @Override
    public CommonResponse<AvatarEntity> uploadAvatar(UploadBody configure)
    {
        AvatarEntity entity = AvatarEntity.builder()
                .type(initializer.getFsConfigure().getType())
                .build();
        try {
            FsRequest fsRequest = getFsRequest(configure.getFile(), configure);
            pluginManager.getPlugin(initializer.getFsConfigure().getType())
                    .ifPresent(plugin -> {
                        FsService fsService = plugin.getService(FsService.class);
                        FsResponse response = fsService.writer(fsRequest);
                        entity.setPath(response.getRemote());
                        entity.setLocal(response.getRemote());
                        if (initializer.getFsConfigure().getType().startsWith("Local")) {
                            entity.setPath(getAccess(entity));
                        }

                        UserEntity user = UserDetailsService.getUser();

                        userRepository.findByCode(user.getCode())
                                .ifPresent(value -> {
                                    value.setAvatarConfigure(entity);
                                    userRepository.save(value);
                                });
                    });
            return CommonResponse.success(entity);
        }
        catch (IOException e) {
            log.error("Failed to upload file [ {} ]", configure.getCode(), e);
            return CommonResponse.failure(e.getMessage());
        }
        finally {
            try {
                configure.getFile().getInputStream().close();
            }
            catch (IOException e) {
                log.warn("Failed to close input stream", e);
            }
        }
    }

    @Override
    public CommonResponse<UserEntity> changeNotify(UserEntity configure)
    {
        return userRepository.findByCode(UserDetailsService.getUser().getCode())
                .map(value -> {
                    value.setNotifyConfigure(configure.getNotifyConfigure());
                    return CommonResponse.success(userRepository.save(value));
                })
                .orElseGet(() -> CommonResponse.failure(ServiceState.USER_NOT_FOUND));
    }

    private FsRequest getFsRequest(MultipartFile file, UploadBody configure)
            throws IOException
    {
        return FsRequest.builder()
                .access(initializer.getFsConfigure().getAccess())
                .secret(initializer.getFsConfigure().getSecret())
                .endpoint(getHome(configure))
                .bucket(initializer.getFsConfigure().getBucket())
                .stream(file.getInputStream())
                .fileName("avatar.png")
                .build();
    }

    private String getHome(UploadBody configure)
    {
        if (!initializer.getFsConfigure().getType().startsWith("Local")) {
            return initializer.getFsConfigure().getEndpoint();
        }
        return String.join(
                "/",
                initializer.getDataHome(),
                UserDetailsService.getUser().getUsername(),
                configure.getMode().toString().toLowerCase()
        );
    }

    private String getAccess(AvatarEntity configure)
    {
        String protocol = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        return protocol + "://" + host + ":" + port + UrlUtils.fixUrl("/upload" + configure.getPath().replaceFirst(initializer.getDataHome(), ""));
    }
}
