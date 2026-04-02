package org.goden.svdemo.service.impl;

import org.goden.svdemo.entity.User;
import org.goden.svdemo.mapper.UserMapper;
import org.goden.svdemo.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper; // 您的数据访问层

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 根据用户名查询用户实体
        User user = userMapper.findUserByUserName(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. 查询该用户拥有的所有权限（或角色）代码
        // 示例1：查询权限码列表（如 ["user:add", "order:query"]）
        List<String> permissionCodeList = userMapper.findPermissionsByUserId(user.getId());
        // 示例2：查询角色码列表（如 ["ROLE_ADMIN", "ROLE_USER"]）
        // List<String> roleCodeList = userMapper.findRolesByUserId(user.getId());

        // 3. 将权限/角色字符串转换为Spring Security认可的GrantedAuthority对象集合
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String permission : permissionCodeList) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        // 如果是角色，转换方式相同。角色本质上是一种特殊的权限，通常以`ROLE_`前缀标识。

        // 4. 构建并返回UserDetails对象
        // 注意：此处密码字段仅用于框架逻辑，在您的JWT方案中实际不会用到，但构造是必须的。
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // 或一个占位符，如""
                .authorities(authorities) // 这是最关键的部分，注入权限集合
                .build();
    }
}
