import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.ment.chat.client.ChatClientApplication;
import com.ment.chat.client.config.CommonTestConfiguration;
import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import com.ment.chat.client.model.out.CreateCompletionByProviderResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.ment.chat.client.controller.ChatController.BASE_PATH;
import static com.ment.chat.client.controller.ChatController.PROVIDER_PROMPT_PATH;
import static com.ment.chat.client.utils.Utility.createObjectMapper;
import static com.ment.chat.client.utils.Utility.readFileResource;
import static com.ment.chat.client.utils.Utility.replaceTemplateValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Integration tests for local profile using WireMock to mock external LLMs done here.
 */
@SpringBootTest(classes = {ChatClientApplication.class, LocalWiremockServerIT.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = {"test", "wiremock"})
@AutoConfigureWireMock(port = 0)
@Tag("target-local")
@AutoConfigureWebTestClient(timeout = "10000")
@AutoConfigureObservability
@EnableAutoConfiguration
@Import({CommonTestConfiguration.class})
@DirtiesContext
class LocalWiremockServerIT {

    private final ObjectMapper mapper = createObjectMapper();

    private static final String IDP_MATCHER_HEADER_KEY = "idp-matcher";
    private static final String CONTENT = "This is mocked llm content!";
    private static final String OPENAI_MODEL = "gpt-5.1-turbo";
    private static final String ANTHROPIC_MODEL = "claude-4.5";
    public static final String TOKEN_USAGE = "promptTokens=10, completionTokens=20, totalTokens=30";

    @Autowired
    WebTestClient client;

    @Autowired
    private WireMockServer wireMockServer;


    @ParameterizedTest
    @CsvSource({
            "ollama-llm-response, payload/chat/create-completion/in/valid_request_ollama.json, 200",
            "docker-llm-response, payload/chat/create-completion/in/valid_request_docker.json, 200",
            "openai-llm-response, payload/chat/create-completion/in/valid_request_openai.json, 200",
    })
    void testLlmResponse_success(String idpMatcher,
                                 String requestFileName,
                                 String httpStatus) throws Exception {
        stubOpenai();

        String requestBody = readFileResource(requestFileName);
        CreateCompletionByProviderRequest request = mapper.readValue(requestBody, CreateCompletionByProviderRequest.class);

        WebTestClient.ResponseSpec responseSpec = client.post().uri(BASE_PATH + PROVIDER_PROMPT_PATH)
                .headers(httpHeaders -> {
                    httpHeaders.add(IDP_MATCHER_HEADER_KEY, idpMatcher);
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                })
                .bodyValue(requestBody)
                .exchange();

        CreateCompletionByProviderResponse response = responseSpec
                .expectStatus().isEqualTo(HttpStatus.valueOf(Integer.parseInt(httpStatus)))
                .expectBody(CreateCompletionByProviderResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getInteractionCompletion().getPromptId()).isNotNull();
        assertThat(response.getInteractionCompletion().getCompletionId()).isNotNull();
        assertThat(response.getInteractionCompletion().getCompletion()).isEqualTo(CONTENT);
        assertThat(response.getInteractionCompletion().getLlm()).isEqualTo(OPENAI_MODEL);
        assertThat(response.getInteractionCompletion().getTokenUsage()).contains(TOKEN_USAGE);
        assertThat(response.getInteractionCompletion().getLlmProvider()).isEqualTo(request.getLlmProvider());
    }

    @ParameterizedTest
    @CsvSource({
            "anthropic-llm-response, payload/chat/create-completion/in/valid_request_anthropic.json, 200",
    })
    void testAnthropicResponse_success(String idpMatcher,
                                       String requestFileName,
                                       String httpStatus) throws Exception {
        stubAnthropic();

        String requestBody = readFileResource(requestFileName);
        CreateCompletionByProviderRequest request = mapper.readValue(requestBody, CreateCompletionByProviderRequest.class);

        WebTestClient.ResponseSpec responseSpec = client.post().uri(BASE_PATH + PROVIDER_PROMPT_PATH)
                .headers(httpHeaders -> {
                    httpHeaders.add(IDP_MATCHER_HEADER_KEY, idpMatcher);
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                })
                .bodyValue(requestBody)
                .exchange();

        CreateCompletionByProviderResponse response = responseSpec
                .expectStatus().isEqualTo(HttpStatus.valueOf(Integer.parseInt(httpStatus)))
                .expectBody(CreateCompletionByProviderResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getInteractionCompletion().getPromptId()).isNotNull();
        assertThat(response.getInteractionCompletion().getCompletionId()).isNotNull();
        assertThat(response.getInteractionCompletion().getCompletion()).isEqualTo(CONTENT);
        assertThat(response.getInteractionCompletion().getLlm()).isEqualTo(ANTHROPIC_MODEL);
        assertThat(response.getInteractionCompletion().getTokenUsage()).contains(TOKEN_USAGE);
        assertThat(response.getInteractionCompletion().getLlmProvider()).isEqualTo(request.getLlmProvider());
    }

    @Disabled("Gemini stubbing is not working, real Gemini call is done since wiremock port can't be set in 1.0.3")
    @ParameterizedTest
    @CsvSource({
            "gemini-llm-response, payload/chat/create-completion/in/valid_request_gemini.json, 200",
    })
    void testGeminiResponse_success(String idpMatcher,
                                       String requestFileName,
                                       String httpStatus) throws Exception {
        stubGemini();

        String requestBody = readFileResource(requestFileName);
        CreateCompletionByProviderRequest request = mapper.readValue(requestBody, CreateCompletionByProviderRequest.class);

        WebTestClient.ResponseSpec responseSpec = client.post().uri(BASE_PATH + PROVIDER_PROMPT_PATH)
                .headers(httpHeaders -> {
                    httpHeaders.add(IDP_MATCHER_HEADER_KEY, idpMatcher);
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                })
                .bodyValue(requestBody)
                .exchange();

        CreateCompletionByProviderResponse response = responseSpec
                .expectStatus().isEqualTo(HttpStatus.valueOf(Integer.parseInt(httpStatus)))
                .expectBody(CreateCompletionByProviderResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getInteractionCompletion().getPromptId()).isNotNull();
        assertThat(response.getInteractionCompletion().getCompletionId()).isNotNull();
        //shows that stubbing is not working since real content is returned
        assertThat(response.getInteractionCompletion().getCompletion()).isNotEqualTo(CONTENT);
        assertThat(response.getInteractionCompletion().getLlmProvider()).isEqualTo(request.getLlmProvider());
    }

    void stubOpenai() throws Exception {
        // Mock the llm API endpoint with the same answer
        String llmResponse = replaceTemplateValue(readFileResource("payload/chat/create-completion/valid_llm_response_openai.json"),
                "content", CONTENT);
        llmResponse = replaceTemplateValue(llmResponse, "model", OPENAI_MODEL);

        wireMockServer.stubFor(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(llmResponse)));

    }

    void stubAnthropic() throws Exception {
        // Mock the llm API endpoint with the same answer
        String llmResponse = replaceTemplateValue(readFileResource("payload/chat/create-completion/valid_llm_response_anthropic.json"),
                "content", CONTENT);
        llmResponse = replaceTemplateValue(llmResponse, "model", ANTHROPIC_MODEL);

        wireMockServer.stubFor(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(llmResponse)));

    }

    void stubGemini() throws Exception {
        String llmResponse = replaceTemplateValue(readFileResource("payload/chat/create-completion/valid_llm_response_gemini.json"),
                "content", CONTENT);
        wireMockServer.stubFor(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(llmResponse)));
    }

}