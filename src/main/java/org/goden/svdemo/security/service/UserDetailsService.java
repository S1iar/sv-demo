package org.goden.svdemo.security.service;


import org.goden.svdemo.security.userdetails.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserDetailsService {
    CustomUserDetails loadUserByUsername(String username);
}
