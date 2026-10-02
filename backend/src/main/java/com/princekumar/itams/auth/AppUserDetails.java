package com.princekumar.itams.auth;

import com.princekumar.itams.user.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our {@link UserAccount} entity to Spring Security's
 * {@link UserDetails} interface. Roles from the entity are prefixed
 * with {@code ROLE_} so {@code @PreAuthorize("hasRole('ADMIN')")} works.
 */
public class AppUserDetails implements UserDetails {

    private final UserAccount userAccount;
    private final List<GrantedAuthority> authorities;

    public AppUserDetails(UserAccount userAccount) {
        this.userAccount = userAccount;
        this.authorities = userAccount.getRoles().stream()
            .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.getCode()))
            .toList();
    }

    public UserAccount getUserAccount() { return userAccount; }
    public Long getUserId()             { return userAccount.getId(); }
    public Long getPersonId()           { return userAccount.getPerson().getId(); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return userAccount.getPasswordHash(); }
    @Override public String getUsername() { return userAccount.getUsername(); }
    @Override public boolean isEnabled()  { return userAccount.isEnabled(); }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
