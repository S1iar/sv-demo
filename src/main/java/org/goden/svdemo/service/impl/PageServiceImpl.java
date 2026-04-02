package org.goden.svdemo.service.impl;

import org.goden.svdemo.entity.User;
import org.goden.svdemo.service.PageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class PageServiceImpl implements PageService {

    @Override
    public List<String> list() {
        // 获取认证主体（即之前注入的UserDetails）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user= (User) userDetails.getAuthorities();
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // 判断是否拥有某个角色或权限
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean canCreateProduct = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("product:create"));


        ArrayList<String> arr = new ArrayList<>();
        arr.add("test");
        arr.add("test1");
        return arr;
    }
}
