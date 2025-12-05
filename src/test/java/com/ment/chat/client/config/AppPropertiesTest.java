package com.ment.chat.client.config;

import com.ment.chat.client.model.enums.LlmProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AppPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.toggle.message-type=true",
        "app.toggle.enable-chat=true",
        "app.toggle.enable-chat-history=false",
        "app.models.ollama.llm-model-name=llama2",
        "app.models.ollama.api-connection.url=http://localhost:11434",
        "app.models.ollama.api-connection.key=test-key",
        "app.models.openai.llm-model-name=gpt-4",
        "app.models.openai.api-connection.url=https://api.openai.com",
        "app.models.openai.api-connection.key=sk-test",
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

        assertThat(appProperties.models().get(LlmProvider.OLLAMA).llmModelName()).isEqualTo("llama2");
        assertThat(appProperties.models().get(LlmProvider.OLLAMA).apiConnection().url()).isEqualTo("http://localhost:11434");
        assertThat(appProperties.models().get(LlmProvider.OPENAI).llmModelName()).isEqualTo("gpt-4");
        assertThat(appProperties.models().get(LlmProvider.ANTHROPIC).llmModelName()).isEqualTo("claude-3");
        assertThat(appProperties.models().get(LlmProvider.DOCKER).llmModelName()).isEqualTo("local-model");
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
        assertThat(violations).hasSize(2);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("models");
    }

    @Test
    void shouldFailValidationWhenToggleAttributesAreNull() {
        AppProperties invalidProperties =
                new AppProperties(new AppProperties.Toggle(null, null, null),
                appProperties.models());

        Set<ConstraintViolation<AppProperties>> violations = validator.validate(invalidProperties);

        assertThat(violations).hasSize(1);
        Iterator<ConstraintViolation<AppProperties>> iterator = violations.iterator();
        assertThat(iterator.next().getPropertyPath().toString()).isEqualTo("toggle.messageType");
    }

    @Test
    void shouldFailValidationWhenModelAttributesAreNull() {
        AppProperties invalidProperties =
                new AppProperties(appProperties.toggle(),
                        new HashMap<>()); //must have at least one element

        Set<ConstraintViolation<AppProperties>> violations = validator.validate(invalidProperties);

        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldFailValidationWhenLeafAttributesIsNull() {
        AppProperties.ApiConnection apiConnection = new AppProperties.ApiConnection("url", "key");
        Map<LlmProvider, AppProperties.ProviderModel> map = new HashMap<>();
        map.put(LlmProvider.OLLAMA, new AppProperties.ProviderModel("llmModelName", apiConnection));
        map.put(LlmProvider.OPENAI, new AppProperties.ProviderModel("llmModelName", apiConnection));
        map.put(LlmProvider.ANTHROPIC, new AppProperties.ProviderModel("llmModelName", apiConnection));
        map.put(LlmProvider.DOCKER, new AppProperties.ProviderModel("llmModelName", new AppProperties.ApiConnection("url", null)));
        AppProperties invalidProperties = new AppProperties(appProperties.toggle(), map);

        Set<ConstraintViolation<AppProperties>> violations = validator.validate(invalidProperties);

        assertThat(violations).hasSize(1);
        Iterator<ConstraintViolation<AppProperties>> iterator = violations.iterator();
        assertThat(iterator.next().getPropertyPath().toString()).isEqualTo("models[DOCKER].apiConnection.key");
    }
}
