import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ment.chat.client.ChatClientApplication;
import com.ment.chat.client.config.CommonTestConfiguration;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
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
import java.util.UUID;

import static com.ment.chat.client.controller.ChatController.BASE_PATH;
import static com.ment.chat.client.controller.ChatController.LLM_PATH;
import static com.ment.chat.client.utils.Utility.readFileResource;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


/**
 * Integration tests for local profile using WireMock to mock external services.
 * Initially no mocking is done of the LLM provider,
 * but tests can be added here later with added profile ,"wiremock".
 */
@SpringBootTest(classes = {ChatClientApplication.class, LocalIT.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = {"test"})
@AutoConfigureWireMock(port = 0)
@Tag("target-local")
@AutoConfigureWebTestClient(timeout = "10000")
@AutoConfigureObservability
@EnableAutoConfiguration
@Import({CommonTestConfiguration.class})
@DirtiesContext
public class LocalIT {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    WebTestClient client;

    @Autowired
    LlmPromptRepository llmPromptRepository;

    @Autowired
    LlmCompletionRepository llmCompletionRepository;

    @Autowired
    CommonTestConfiguration.WireMockAdminClient wireMockAdminClient;

    private static final String CORRELATION_ID = UUID.randomUUID().toString();
    private static final String IDP_MATCHER_HEADER_KEY = "idp-matcher";


    @ParameterizedTest
    @CsvSource({
            "provide-missing-prompt, payload/chat/create-completion/in/missing_prompt.json, payload/chat/create-completion/out/bad_request.json, 400",})
    void testMissingPrompt_badRequest_createCompletion(String idpMatcher,
                                                       String requestFileName,
                                                       String responseFileName,
                                                       String httpStatus) throws Exception {
        String requestBody = readFileResource(requestFileName);
        WebTestClient.ResponseSpec response = client.post().uri(BASE_PATH + LLM_PATH)
                .headers(httpHeaders -> {
                    httpHeaders.add(IDP_MATCHER_HEADER_KEY, idpMatcher);
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                    httpHeaders.add("Correlation-Id", CORRELATION_ID);
                })
                .bodyValue(requestBody)
                .exchange();

        byte[] responseBody = response.expectStatus().isEqualTo(HttpStatus.valueOf(Integer.parseInt(httpStatus)))
                .expectBody()
                .returnResult()
                .getResponseBody();

        String expectedJson = readFileResource(responseFileName);
        String responseJson = Objects.requireNonNull(new String(responseBody, StandardCharsets.UTF_8));

// Remove timestamp or other dynamic fields before comparison

        JsonNode expected = removeNodes(expectedJson, mapper, "timestamp");
        JsonNode actual = removeNodes(responseJson, mapper, "timestamp");

        JSONAssert.assertEquals(expected.toString(), actual.toString(), JSONCompareMode.LENIENT);
    }

    private JsonNode removeNodes(String json, ObjectMapper myMapper, String... fieldNames) throws JsonProcessingException {
        JsonNode node = myMapper.readTree(json);
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                ((ObjectNode) node).remove(fieldName);
            }
        }
        return node;
    }

}
