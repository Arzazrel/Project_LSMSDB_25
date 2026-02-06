package it.unipi.myfuture.myfuture_backend.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserPrincipal implements UserDetails {
    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;          // 'true' if account is active, 'false' if account is deleted (soft)
    private final boolean accountNonLocked; // 'true' if account is suspended, 'false' if account isn't suspended (soft)

    public UserPrincipal(Long id, String email, String password, Collection<? extends GrantedAuthority> authorities,
                         boolean enabled, boolean accountNonLocked) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
    }

    public Long getId() { return id; }                        // added for Redis use

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    // method for implemented authentication functionalities in the system.
    @Override public boolean isEnabled() { return enabled; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }


    // method for not implemented functionalities in this system. Return true to allow authentication.
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

}
