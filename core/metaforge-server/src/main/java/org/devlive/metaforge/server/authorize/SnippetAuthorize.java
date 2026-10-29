package org.devlive.metaforge.server.authorize;

import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.repository.SnippetRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service(value = "snippetAuthorize")
public class SnippetAuthorize
{
    private final SnippetRepository snippetRepository;

    public SnippetAuthorize(SnippetRepository snippetRepository)
    {
        this.snippetRepository = snippetRepository;
    }

    public boolean validateById(Long id)
            throws UserNotEqualsException
    {
        try {
            UserEntity loginUser = UserDetailsService.getUser();
            if (Objects.equals(loginUser.getId(), this.snippetRepository.findById(id).get().getUserId())) {
                return true;
            }
            throw new UserNotEqualsException(ServiceState.USER_UNAUTHORIZED.getValue());
        }
        catch (Exception ex) {
            throw new UserNotEqualsException(ServiceState.USER_UNAUTHORIZED.getValue());
        }
    }
}
