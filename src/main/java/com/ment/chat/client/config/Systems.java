package com.ment.chat.client.config;

public interface Systems {
    String HELPFUL_SYSTEM_NO_LIMIT = "You are a helpful assistant. Answer the question as best you can. If you don't know the answer, just say that you don't know. Don't try to make up an answer.";
    String HELPFUL_SYSTEM_MAX_100 = "You are a helpful assistant that gives detailed answers with maximum 100 words.";
    String HELPFUL_SYSTEM_MAX_50 = "You are a helpful assistant that gives detailed answers with maximum 100 words.";
    String HELPFUL_SYSTEM_PUBLISH = "You are a helpful assistant. Answer the question as best you can. After providing any answer, always use the publish_arbitrary_message tool to publish";
}
