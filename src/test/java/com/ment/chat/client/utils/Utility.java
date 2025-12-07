package com.ment.chat.client.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utility {
    public static String readFileResource(String fileName) throws URISyntaxException, IOException {
        return Files.readString(Paths.get(Objects.requireNonNull(Utilities.class.getClassLoader().getResource(fileName)).toURI()), StandardCharsets.UTF_8);
    }

    public static String replaceTemplateValue(String input, String key, String value) {
        return StringSubstitutor.replace(input, Map.of(key, value), "{{", "}}");
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

}
