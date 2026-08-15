package org.goden.svdemo.service;

public interface PasswordService {
    String encodePassword(String rawPassword);

    String matches(String encodedPassword);
}
