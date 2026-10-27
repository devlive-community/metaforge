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
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.entity.itransient.user.UserEditorEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.model.AiModel;
import org.devlive.metaforge.service.record.TreeRecord;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.repository.jdbc.MenuAccessRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.JwtService;
import org.devlive.metaforge.service.service.UserService;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisTemplate redisTemplate;
    private final Environment environment;
    private final InitializerConfigure initializer;
    private final PluginManager pluginManager;
    private final HttpServletRequest request;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, SourceRepository sourceRepository, MenuAccessRepository menuRepository, PasswordEncoder encoder, AuthenticationManager authenticationManager, JwtService jwtService, RedisTemplate redisTemplate, Environment environment, InitializerConfigure initializer, PluginManager pluginManager, HttpServletRequest request)
    {
        this.userRepository = userRepository;
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
    public CommonResponse<UserEntity> saveOrUpdate(UserEntity configure)
    {
        if (ObjectUtils.isEmpty(configure.getId())) {
            Optional<UserEntity> userOptional = this.userRepository.findByUsername(configure.getUsername());
            if (userOptional.isPresent()) {
                return CommonResponse.failure(ServiceState.USER_EXISTS);
            }
            configure.setCode(CodeUtils.generateCode(false));
            configure.setUsername(configure.getUsername());
            configure.setPassword(encoder.encode(configure.getPassword()));
            Set<RoleEntity> userRoles = configure.getRoles();
            Set<RoleEntity> roles = new HashSet<>();
            if (ObjectUtils.isEmpty(userRoles)) {
                Optional<RoleEntity> userRoleOptional = roleRepository.findByName("User");
                if (userRoleOptional.isEmpty()) {
                    return CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND);
                }
                roles.add(userRoleOptional.get());
            }
            configure.setRoles(roles);
        }
        else {
            userRepository.findById(configure.getId())
                    .ifPresent(value -> {
                        NullAwareBeanUtils.copyNullProperties(value, configure);
                        if (StringUtils.isEmpty(value.getCode())) {
                            value.setCode(CodeUtils.generateCode(false));
                        }
                    });
            configure.setRoles(configure.getRoles());
        }
        return CommonResponse.success(userRepository.save(configure));
    }

    @Override
    public CommonResponse<UserEntity> saveOrUpdate(BaseRepository<UserEntity, Long> repository, UserEntity configure)
    {
        if (configure.getId() != null) {
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
    public CommonResponse<UserEntity> info(String code)
    {
        return userRepository.findByCode(UserDetailsService.getUser().getCode())
                .map(CommonResponse::success)
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
        return CommonResponse.success(PageEntity.build(this.userRepository.findFiltered(filter, UserDetailsService.getUser())));
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
