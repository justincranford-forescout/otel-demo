package com.justincranford.oteldemo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class SecureRandomUtil {
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();
}
