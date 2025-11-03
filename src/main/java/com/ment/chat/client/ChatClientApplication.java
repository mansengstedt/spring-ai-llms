package com.ment.chat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Arrays;

@SpringBootApplication
@ConfigurationPropertiesScan
@Slf4j
public class ChatClientApplication {

    private final boolean showBeans = false;

    static void main(String[] args) {

        clearSslLogging();

        var ctx = SpringApplication.run(ChatClientApplication.class, args);

        if (ctx.getBean(ChatClientApplication.class).showBeans) {
            Arrays.stream(ctx.getBeanDefinitionNames())
                    .forEach(beanName -> log.info("Bean: {}", beanName));
        }

    }

    /**
     * To avoid logging of SSL information like cipher suites and ssl timeouts.
     */
    private static void clearSslLogging() {
        // Remove JSSE debug if set via JVM arg
        System.clearProperty("javax.net.debug");

        // Lower java.util.logging verbosity for JSSE internals
        java.util.logging.Logger.getLogger("sun.security.ssl")
                .setLevel(java.util.logging.Level.WARNING);
        java.util.logging.Logger.getLogger("javax.net.ssl")
                .setLevel(java.util.logging.Level.WARNING);
    }
}
