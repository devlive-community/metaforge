package org.devlive.metaforge.server.authorize;

import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.service.body.BaseUserBody;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service(value = "userAuthorize")
public class UserAuthorize
{
    private final SourceRepository sourceRepository;

    public UserAuthorize(SourceRepository sourceRepository)
    {
        this.sourceRepository = sourceRepository;
    }

    public boolean validateUser(BaseUserBody configure)
            throws UserNotEqualsException
    {
        try {
            UserEntity loginUser = UserDetailsService.getUser();
            if (Objects.equals(loginUser.getId(), configure.getUserId())) {
                return true;
            }
            throw new UserNotEqualsException(ServiceState.USER_UNAUTHORIZED.getValue());
        }
        catch (Exception ex) {
            throw new UserNotEqualsException(ServiceState.USER_UNAUTHORIZED.getValue());
        }
    }

    public boolean validateSource(Long id)
            throws UserNotEqualsException
    {
        try {
            UserEntity loginUser = UserDetailsService.getUser();
            if (Objects.equals(loginUser.getId(), this.sourceRepository.findById(id).get().getUserId())) {
                return true;
            }
            throw new UserNotEqualsException(ServiceState.USER_UNAUTHORIZED.getValue());
        }
        catch (Exception ex) {
            throw new UserNotEqualsException(ServiceState.USER_UNAUTHORIZED.getValue());
        }
    }
}
