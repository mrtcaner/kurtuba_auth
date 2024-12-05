package com.kurtuba.auth.data.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class KurtubaUserDetails implements UserDetails {

    private User user;

    public KurtubaUserDetails(User user) {
        this.user = user;
    }

    public String getId(){
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getUserRoles().stream()
                .map(auth -> new SimpleGrantedAuthority(auth.getRole().name())).toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {return !user.isLocked();}

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActivated();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KurtubaUserDetails) {
            return user.getId().equals( ((KurtubaUserDetails) obj).getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return user.getId() != null ? user.getId().hashCode() : 0;
    }

}
