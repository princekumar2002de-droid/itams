package com.princekumar.itams.auth;

import com.princekumar.itams.user.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repo;

    public AppUserDetailsService(UserAccountRepository repo) { this.repo = repo; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repo.findByUsername(username)
            .map(AppUserDetails::new)
            // Deliberately generic message — do not leak whether username exists.
            .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
    }
}
