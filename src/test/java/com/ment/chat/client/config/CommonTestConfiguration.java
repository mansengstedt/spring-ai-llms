package com.ment.chat.client.config;

//import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestConfiguration
public class CommonTestConfiguration {
    //@Bean
    //WireMockConfigurationCustomizer optionsCustomizer() {
        //return config -> config.notifier(new ConsoleNotifier(true));
    //}

    //@Bean
    WireMockAdminClient wireMockClient(@Value("${wiremock.server.port}") String wireMockPort) {
        return new WireMockAdminClient(WebTestClient.bindToServer().baseUrl("http://localhost:" + wireMockPort).build());
    }

    public record WireMockAdminClient(WebTestClient webTestClient) {
    }


}
