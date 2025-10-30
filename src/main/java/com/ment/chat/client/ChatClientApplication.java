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
        var ctx = SpringApplication.run(ChatClientApplication.class, args);
        if (ctx.getBean(ChatClientApplication.class).showBeans) {
            Arrays.stream(ctx.getBeanDefinitionNames())
                    .forEach(beanName -> log.info("Bean: " + beanName));
        }

    }
}
