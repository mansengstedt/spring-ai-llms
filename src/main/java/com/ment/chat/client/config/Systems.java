package com.ment.chat.client.config;

public interface Systems {
    String HELPFUL_SYSTEM_NO_LIMIT = "You are a helpful assistant. Answer the question as best you can. If you don't know the answer, just say that you don't know. Don't try to make up an answer.";
    String HELPFUL_SYSTEM_MAX_100 = "You are a helpful assistant that gives detailed answers with maximum 100 words.";
    String HELPFUL_SYSTEM_MAX_50 = "You are a helpful assistant that gives detailed answers with maximum 100 words.";
    String HELPFUL_SYSTEM_PUBLISH = "You are a helpful assistant. Answer the question as best you can. After providing any answer, always use the publish_arbitrary_message tool to publish";
    String SUMMARY_SYSTEM_FROM_LLMS = """
            Make a summary of the answers from a set of LLM model providers like GEMINI, ANTROPIC, OPENAI etc.
            Each given answer used as input text is tagged with the corresponding LLM model provider in capital letters like 'GEMINI: answer1', 'ANTHROPIC: answer2'.
            The returned summary length should not be longer than 67% of the total input prompt length.
            The summary shall start with 'According to GEMINI and ANTHROPIC:' if these are the tags used in the input answers.
            After this source prefix comes the real summary using only the input answers and nothing else.
            """;

}
