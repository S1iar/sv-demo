package org.goden.svdemo.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserDetailsService {
    UserDetails loadUserByUserID(Integer username);
}
