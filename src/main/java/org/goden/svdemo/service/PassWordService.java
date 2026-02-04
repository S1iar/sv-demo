package org.goden.svdemo.service;

public interface PassWordService {
    String encodePassword(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
