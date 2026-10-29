package org.devlive.metaforge.service.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class UserDetailsServiceImpl
        implements org.springframework.security.core.userdetails.UserDetailsService
{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LinkRepository linkRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, RoleRepository roleRepository, LinkRepository linkRepository)
    {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.linkRepository = linkRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException
    {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        Set<RoleEntity> roles = new HashSet<>();
        roleRepository.findAllById(linkRepository.roleIdsOfUser(user.getId())).forEach(roles::add);
        user.setRoles(roles);
        return UserDetailsService.build(user);
    }
}
