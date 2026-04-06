package com.ment.chat.client.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublisherTool {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * GEMINI, OPENAI, GROK, ANTHROPIC, OLLAMA will execute this tool call.
     * GEMINI, ANTHROPIC will set the model name that might differ from the configured model name.
     * OPENAI, GROK will not know the model name, ´unknown´ is then used as specified.
     * OLLAMA will have a null name instead of ´unknown´.
     * DOCKER will not even execute the tool call.
     *
     * @param modelName the model used by the provider, ´unknown´sometimes
     * @param message the prompt answer
     * @param context used to retrieve the configured model from provider initialization
     * @return the prompt answer
     */
    @Tool(name = "publish_arbitrary_message",
            description = """
                    You MUST always call the publish_arbitrary_message tool with your answer before responding.
                    Publish the EXACT prompt answer given by the LLM
                    via the spring event bus for other clients to use.
                    """,
            //returnDirect = false leads to infinite tool calls for GROK
            //true means that the result of the tool call is not used by the LLM and is directly returned to the client
            returnDirect = true)
    String publishAnswer(@ToolParam(description = """
                                 The actual model name used by the provider should ALWAYS be used.
                                 If the model name used for answering is not known, use 'unknown'.,
                                 """) String modelName,
                         @ToolParam(description = "The returned answer to the prompt.") String message,
                         ToolContext context) {

        String configuredModelName = (String) context.getContext().get("configuredModelName");
        log.info("configured model: {}, used model: {}, published message by tool: {}", configuredModelName, modelName, message);
        applicationEventPublisher.publishEvent(message);
        return message;
    }

}
