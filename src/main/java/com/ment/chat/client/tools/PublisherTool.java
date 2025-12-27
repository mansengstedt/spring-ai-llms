package com.ment.chat.client.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublisherTool {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Tool(name = "publish_arbitrary_message",
            description = """
                    Publish the EXACT prompt answer given by the LLM
                    via the spring event bus for other clients to use
                    """)
    void publishAnswer(@ToolParam(description = "The model name used for answering like gpt-4.1") String modelName,
                       @ToolParam(description = "The returned answer to the prompt") String message) {

        String output = modelName + " used! " + "Published message by publishAnswer tool:" + message;
        log.info(output);
        applicationEventPublisher.publishEvent(message);
    }

}
