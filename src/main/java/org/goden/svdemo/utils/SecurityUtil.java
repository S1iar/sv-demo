package org.goden.svdemo.utils;

import org.goden.svdemo.security.userdetails.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /** 获取当前登录用户的 CustomUserDetails，未登录返回 null */
    public static CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        return (CustomUserDetails) auth.getPrincipal();
    }

    /** 获取当前登录用户的ID，未登录返回 null */
    public static Long getCurrentUserId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /** 获取当前登录用户的用户名，未登录返回 null */
    public static String getCurrentUsername() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }
}
