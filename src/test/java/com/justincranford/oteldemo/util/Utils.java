package com.justincranford.oteldemo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class Utils {
    public static @NotNull String prefixAllLines(final String prefix, final String content) {
        return prefix + ">>>" + content.replaceAll("(\r)?\n(\r)?", "$1\n$2" + prefix+ ">>> ");
    }

    public static String readFileContents(String otelContribYamlFilePath, final Charset charset) {
        try {
            return Files.readString(Path.of(otelContribYamlFilePath), charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeFileContents(String otelContribYamlFileUpdatedContents) {
        try {
            final Path tempOtelConfig = Files.createTempFile("otel-config-", ".yaml");
            Files.writeString(tempOtelConfig, otelContribYamlFileUpdatedContents, StandardCharsets.UTF_8);
            return tempOtelConfig.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
