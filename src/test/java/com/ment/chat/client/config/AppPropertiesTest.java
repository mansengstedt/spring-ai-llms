package com.ment.chat.client.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest(classes = AppPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.toggle.message-type=true",
        "app.toggle.enable-chat=true",
        "app.toggle.enable-chat-history=false",
        "app.models.ollama.llm-model-name=llama2",
        "app.models.ollama.api-connection.url=http://localhost:11434",
        "app.models.ollama.api-connection.key=test-key",
        "app.models.open-ai.llm-model-name=gpt-4",
        "app.models.open-ai.api-connection.url=https://api.openai.com",
        "app.models.open-ai.api-connection.key=sk-test",
        "app.models.anthropic.llm-model-name=claude-3",
        "app.models.anthropic.api-connection.url=https://api.anthropic.com",
        "app.models.anthropic.api-connection.key=ant-test",
        "app.models.docker.llm-model-name=local-model",
        "app.models.docker.api-connection.url=http://localhost:8080",
        "app.models.docker.api-connection.key=docker-key"
})
class AppPropertiesTest {

    @EnableConfigurationProperties(AppProperties.class)
    static class TestConfig {
    }

    @Autowired
    private AppProperties appProperties;

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldLoadValidConfiguration() {
        assertThat(appProperties).isNotNull();
        assertThat(appProperties.toggle().messageType()).isTrue();
        assertThat(appProperties.toggle().enableChat()).isTrue();
        assertThat(appProperties.toggle().enableChatHistory()).isFalse();

        assertThat(appProperties.models().ollama().llmModelName()).isEqualTo("llama2");
        assertThat(appProperties.models().ollama().apiConnection().url()).isEqualTo("http://localhost:11434");
        assertThat(appProperties.models().openAi().llmModelName()).isEqualTo("gpt-4");
        assertThat(appProperties.models().anthropic().llmModelName()).isEqualTo("claude-3");
        assertThat(appProperties.models().docker().llmModelName()).isEqualTo("local-model");
    }

    @Test
    void shouldValidateSuccessfully() {
        Set<ConstraintViolation<AppProperties>> violations = validator.validate(appProperties);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidationWhenToggleIsNull() {
        AppProperties invalidProperties = new AppProperties(null, appProperties.models());

        Set<ConstraintViolation<AppProperties>> violations = validator.validate(invalidProperties);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("toggle");
    }

    @Test
    void shouldFailValidationWhenModelsIsNull() {
        AppProperties invalidProperties = new AppProperties(appProperties.toggle(), null);

        Set<ConstraintViolation<AppProperties>> violations = validator.validate(invalidProperties);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("models");
    }

    @Test
    void shouldFailValidationWhenToggleAttributesAreNull() {
        AppProperties invalidProperties =
                new AppProperties(new AppProperties.Toggle(null, null, null),
                appProperties.models());

        Set<ConstraintViolation<AppProperties>> violations = validator.validate(invalidProperties);

        assertThat(violations).hasSize(3);
        Iterator<ConstraintViolation<AppProperties>> iterator = violations.iterator();
        assertThat(iterator.next().getPropertyPath().toString()).startsWith("toggle.");
        assertThat(iterator.next().getPropertyPath().toString()).startsWith("toggle.");
        assertThat(iterator.next().getPropertyPath().toString()).startsWith("toggle.");
    }

    @Test
    void shouldFailValidationWhenModelAttributesAreNull() {
        AppProperties invalidProperties =
                new AppProperties(appProperties.toggle(),
                        new AppProperties.Models(null, null, null, null));

        Set<ConstraintViolation<AppProperties>> violations = validator.validate(invalidProperties);

        assertThat(violations).hasSize(4);
        Iterator<ConstraintViolation<AppProperties>> iterator = violations.iterator();
        assertThat(iterator.next().getPropertyPath().toString()).startsWith("models.");
        assertThat(iterator.next().getPropertyPath().toString()).startsWith("models.");
        assertThat(iterator.next().getPropertyPath().toString()).startsWith("models.");
        assertThat(iterator.next().getPropertyPath().toString()).startsWith("models.");
    }

    @Test
    void shouldFailValidationWhenLeafAttributesIsNull() {
        AppProperties.Models.ApiConnection apiConnection = new AppProperties.Models.ApiConnection("url", "key");
        AppProperties invalidProperties =
                new AppProperties(appProperties.toggle(),
                        new AppProperties.Models(new AppProperties.Models.Ollama("llmModelName", apiConnection),
                                new AppProperties.Models.OpenAi("llmModelName", apiConnection),
                                new AppProperties.Models.Anthropic("llmModelName", apiConnection),
                                new AppProperties.Models.Docker("llmModelName", new AppProperties.Models.ApiConnection("url", null))));

        Set<ConstraintViolation<AppProperties>> violations = validator.validate(invalidProperties);

        assertThat(violations).hasSize(1);
        Iterator<ConstraintViolation<AppProperties>> iterator = violations.iterator();
        assertThat(iterator.next().getPropertyPath().toString()).isEqualTo("models.docker.apiConnection.key");
    }
}
