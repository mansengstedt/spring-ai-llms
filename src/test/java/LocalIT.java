import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ment.chat.client.ChatClientApplication;
import com.ment.chat.client.config.CommonTestConfiguration;
import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import org.json.JSONException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
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

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.ment.chat.client.controller.ChatController.BASE_PATH;
import static com.ment.chat.client.controller.ChatController.CHAT_PATH;
import static com.ment.chat.client.controller.ChatController.LLM_PATH;
import static com.ment.chat.client.controller.ChatController.PROMPT_PATH;
import static com.ment.chat.client.model.enums.LlmProvider.ANTHROPIC;
import static com.ment.chat.client.utils.Utility.createObjectMapper;
import static com.ment.chat.client.utils.Utility.readFileResource;
import static com.ment.chat.client.utils.Utility.replaceTemplateValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


/**
 * Integration tests for local profile using WireMock to mock external services.
 * Initially no stubbing is done of the LLM provider but wiremock profile is still needed.
 */
@SpringBootTest(classes = {ChatClientApplication.class, LocalIT.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = {"test", "wiremock"})
@AutoConfigureWireMock(port = 0)
@Tag("target-local")
@AutoConfigureWebTestClient(timeout = "10000")
@AutoConfigureObservability
@EnableAutoConfiguration
@Import({CommonTestConfiguration.class})
@DirtiesContext
public class LocalIT {

    private final ObjectMapper mapper = createObjectMapper();

    private static final String IDP_MATCHER_HEADER_KEY = "idp-matcher";

    @Autowired
    WebTestClient client;


    @ParameterizedTest
    @CsvSource({
            "missing-prompt, payload/chat/create-completion/in/missing_prompt.json, payload/chat/create-completion/out/missing_prompt.json, 400",
            "too-short-prompt, payload/chat/create-completion/in/too_short_prompt.json, payload/chat/create-completion/out/too_short_prompt.json, 400",
            "invalid-provider, payload/chat/create-completion/in/invalid_provider.json, payload/chat/create-completion/out/invalid_provider.json, 400",
        })
    void testMissingPrompt_badRequest_createCompletion(String idpMatcher,
                                                       String requestFileName,
                                                       String responseFileName,
                                                       String httpStatus) throws Exception {
        String requestBody = readFileResource(requestFileName);
        WebTestClient.ResponseSpec response = client.post().uri(BASE_PATH + LLM_PATH)
                .headers(httpHeaders -> {
                    httpHeaders.add(IDP_MATCHER_HEADER_KEY, idpMatcher);
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                })
                .bodyValue(requestBody)
                .exchange();

        byte[] actualResponseBody = response
                .expectStatus().isEqualTo(HttpStatus.valueOf(Integer.parseInt(httpStatus)))
                .expectBody()
                .returnResult()
                .getResponseBody();

        String expectedJson = readFileResource(responseFileName);
        String responseJson = new String(Objects.requireNonNull(actualResponseBody), StandardCharsets.UTF_8);

        JsonNode expected = mapper.readTree(expectedJson);
        JsonNode actual = mapper.readTree(responseJson);

        JSONAssert.assertEquals(expected.toString(), actual.toString(), JSONCompareMode.LENIENT);
    }

    @ParameterizedTest
    @CsvSource({
            "empty-ollama-response, payload/chat/create-completion/in/valid_request_ollama.json, 200",
            "empty-docker-response, payload/chat/create-completion/in/valid_request_docker.json, 200",
            "empty-openai-response, payload/chat/create-completion/in/valid_request_openai.json, 200",
            "empty-anthropic-response, payload/chat/create-completion/in/valid_request_anthropic.json, 500",
    })
    void testEmptyLlmResponse_success(String idpMatcher,
                                         String requestFileName,
                                         String httpStatus) throws Exception {


        String requestBody = readFileResource(requestFileName);
        CreateCompletionByProviderRequest request = mapper.readValue(requestBody, CreateCompletionByProviderRequest.class);

        WebTestClient.ResponseSpec responseSpec = client.post().uri(BASE_PATH + LLM_PATH)
                .headers(httpHeaders -> {
                    httpHeaders.add(IDP_MATCHER_HEADER_KEY, idpMatcher);
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                })
                .bodyValue(requestBody)
                .exchange();

        if (httpStatus.equals("500")) {
            //Anthropic empty response currently gives 500 error
            assertThat(request.getLlmProvider()).isEqualTo(ANTHROPIC);
            responseSpec.expectStatus().is5xxServerError();
            return;
        }

        CreateCompletionResponse response = responseSpec
                .expectStatus().isEqualTo(HttpStatus.valueOf(Integer.parseInt(httpStatus)))
                .expectBody(CreateCompletionResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getInteractionCompletion().getPromptId()).isNotNull();
        assertThat(response.getInteractionCompletion().getCompletionId()).isNotNull();
        assertThat(response.getInteractionCompletion().getCompletion()).isNull();
        assertThat(response.getInteractionCompletion().getLlmProvider()).isEqualTo(request.getLlmProvider());
    }

    @ParameterizedTest
    @CsvSource({
            "unknown-prompt-id, 1f3fa64a-295e-4085-9e4e-524583aeaa3f, payload/chat/find-prompt/out/prompt_id_not_found.json, 404",
            "invalid-prompt-id, 1f3fa64a-295e-4085-9e4e-524583aeaa3, payload/chat/find-prompt/out/invalid_prompt_id.json, 400",})
    void testPromptId_badRequest_findPrompt(String idpMatcher,
                                                       String promptId,
                                                       String responseFileName,
                                                       String httpStatus) throws Exception {
        String expectedJson = replaceTemplateValue(readFileResource(responseFileName), "prompt_id", promptId);
        String path = BASE_PATH + PROMPT_PATH + "/" + promptId;

        compareResults(path, idpMatcher, httpStatus, expectedJson);
    }

    @ParameterizedTest
    @CsvSource({
            "unknown-chat-id, xxxxxx, payload/chat/find-chat/out/chat_id_not_found.json, 404",
            "invalid-chat-id, xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, payload/chat/find-chat/out/invalid_chat_id.json, 400",})
    void testChatId_badRequest_findChat(String idpMatcher,
                                            String chatId,
                                            String responseFileName,
                                            String httpStatus) throws Exception {
        String expectedJson = replaceTemplateValue(readFileResource(responseFileName), "chat_id", chatId);
        String path = BASE_PATH + CHAT_PATH + "/" + chatId;

        compareResults(path, idpMatcher, httpStatus, expectedJson);
    }

    private void compareResults(String path, String idpMatcher, String httpStatus, String expectedJson) throws JsonProcessingException, JSONException {
        byte[] actualResponseBody = client.get().uri(path)
                .headers(httpHeaders -> {
                    httpHeaders.add(IDP_MATCHER_HEADER_KEY, idpMatcher);
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                })
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.valueOf(Integer.parseInt(httpStatus)))
                .expectBody()
                .returnResult()
                .getResponseBody();

        String responseJson = new String(Objects.requireNonNull(actualResponseBody), StandardCharsets.UTF_8);

        JsonNode expected = mapper.readTree(expectedJson);
        JsonNode actual = mapper.readTree(responseJson);

        JSONAssert.assertEquals(expected.toString(), actual.toString(), JSONCompareMode.LENIENT);
    }

}
