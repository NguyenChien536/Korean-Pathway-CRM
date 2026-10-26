package com.crm.korean_pathway_crm.auth.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtPropertiesTest {
    @TempDir Path directory;

    @Test
    void validatesKeyByUtf8BytesInsteadOfCharacterCount() {
        JwtProperties properties = new JwtProperties();
        properties.setSignerKey("é".repeat(32));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties)).isEmpty();
            assertThat(properties.signingKeyBytes()).hasSize(64);

            properties.setSignerKey("short-key");
            assertThat(factory.getValidator().validate(properties)).isNotEmpty();
            assertThrows(IllegalStateException.class, properties::signingKeyBytes);
        }
    }

    @Test
    void importsPersistentLocalKeyWithoutStartingDatabase() throws Exception {
        Path file = directory.resolve("jwt.properties");
        Files.writeString(file, "jwt.signer-key=" + "random-test-key!!".repeat(4));

        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.import=" + file.toUri())
                .withUserConfiguration(PropertiesConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).signingKeyBytes()).hasSize(68);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class PropertiesConfiguration {}
}
