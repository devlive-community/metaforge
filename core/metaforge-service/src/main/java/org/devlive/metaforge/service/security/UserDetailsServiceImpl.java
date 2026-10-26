package org.devlive.metaforge.service.security;

import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class UserDetailsServiceImpl
        implements org.springframework.security.core.userdetails.UserDetailsService
{
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException
    {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        return UserDetailsService.build(user);
    }
}
