package com.justincranford.oteldemo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class Utils {
    public static @NotNull String prefixAllLines(final String prefix, final String content) {
        return prefix + " >>>" + content.replaceAll("(\r)?\n(\r)?", "$1\n$2" + prefix+ ">>> ");
    }
}
