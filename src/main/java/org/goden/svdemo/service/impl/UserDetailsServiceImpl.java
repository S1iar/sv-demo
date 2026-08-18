package org.goden.svdemo.service.impl;

import org.goden.svdemo.entity.User;
import org.goden.svdemo.entity.Role;
import org.goden.svdemo.entity.Permission;
import org.goden.svdemo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findUserByUserName(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }

        Collection<GrantedAuthority> authorities = buildAuthorities(user.getId());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }

    public UserDetails loadUserByUserID(Integer id) throws UsernameNotFoundException {
        User user = userMapper.findUserById(id);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在，ID：" + id);
        }
        Collection<GrantedAuthority> authorities = buildAuthorities(user.getId());
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }

    private Collection<GrantedAuthority> buildAuthorities(Integer userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 角色（加 ROLE_ 前缀）
        List<Role> roles = userMapper.findRolesByUserId(userId);
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
        }

        // 细粒度权限
        List<Permission> permissions = userMapper.findPermissionsByUserId(userId);
        for (Permission perm : permissions) {
            authorities.add(new SimpleGrantedAuthority(perm.getPermCode()));
        }

        return authorities;
    }
}