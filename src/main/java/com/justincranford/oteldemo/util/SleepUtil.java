package com.justincranford.oteldemo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.lang.Thread.sleep;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
@Slf4j
public class SleepUtil {
    public static boolean waitMillis(final String thing, final long millis) {
        try {
            sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            log.warn("Interrupted {}", thing);
            return true;
        }
        return false;
    }
}
