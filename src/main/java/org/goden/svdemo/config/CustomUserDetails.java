package org.goden.svdemo.config;

import org.goden.svdemo.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user; // 存储完整的用户对象
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this(user, Collections.emptyList());
    }

    public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    // UserDetails接口必须实现的方法
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // 从自定义用户对象获取
    }

    @Override
    public String getUsername() {
        return user.getUsername(); // 从自定义用户对象获取
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled(); // 假设1表示启用
    }
}
