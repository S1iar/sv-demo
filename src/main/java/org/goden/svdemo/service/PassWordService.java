package org.goden.svdemo.service;

public interface PassWordService {
    String encodePassword(String rawPassword);

    String matches(String encodedPassword);
}
