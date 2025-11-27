# Chatbot client 

This service as a client, uses many different tools for chatting with different LLMs like
Ollama, Docker, Anthropic and OpenAI.

### Ollama
Install Ollama and download some models.

Available models are: qwen3, llama3, ...

To install and download Ollama locally with some models, see https://ollama.com/docs/installation

Ollama API key is not needed.

### Docker
Install Docker LLMs with downloaded models.

Available models are: gemma3.2, llama3.2, deepseek-r1-distill-llama, ...

To install and download Docker with some models, see https://docs.docker.com/ai/model-runner/get-started/

Specifically, add TCP support with port 12434.

Docker API key is not needed.

### OpenAI
LLMs using OpenAI API.

Available models are: gpt-4o, gpt-4o-mini, gpt-4.1-nano, o4-mini, gpt-5, gpt-5-nano, ...
see https://platform.openai.com/docs/models

OpenApi account with quota must be opened and a key is generated for external clients to use, see https://platform.openai.com/signup,
If quota is exceeded/unavailable, the service will return a 429 error code.

The OpenAI API key can be generated on the following page: https://platform.openai.com/api-keys
The generated key is read outside the app in env variable `OPEN_AI_CONNECTION_KEY`.

To see OpenApi account usage, goto https://platform.openai.com/usage

Client certificate is not used for external LLMs, but it can be configured in the application.yml file.

### Anthropic
LLMs using Anthropic API.

Available models are: claude-sonnet-4-20250514, claude-sonnet-4-5-20250929, ...
see https://www.anthropic.com/claude/sonnet

Anthropic account with quota must be opened and a key is generated for external clients to use, 
see https://console.anthropic.com/docs/en/home or https://console.anthropic.com/settings/organization.
If quota is exceeded/unavailable, the service will return a 429 error code.

The Anthropic API key can be generated on the following page: https://console.anthropic.com/settings/keys
The generated key is read outside the app in env variable `ANTHROPIC_CONNECTION_KEY`.

To see OpenApi account usage, goto https://console.anthropic.com/settings/billing or https://console.anthropic.com/usage


## Service End points

* POST /chat/haiku?provider=ANTHROPIC create a haiku from Anthropic with given parameters from Docker LLMs
* POST /chat/llm?provider=OLLAMA chat with internal LLMs using Ollama
* POST /chat/llm?provider=DOCKER chat with docker LLMs using Docker
* POST /chat/llm?provider=OPENAI chat with external LLMs using OpenAI
* POST /chat/llm?provider=ANTHROPIC chat with external LLMs using Anthropic
* POST /chat/llm/all chat with all LLMs and return all answers
* GET /chat/prompt/{prompt-id} get llmPrompt and completions from given llmPromptId
* GET /chat/chat/{chat-id} get chat by chatId
* GET /chat/status get chat service status for all LLMs


## Model
The main domain objects are:
* Prompt - the prompt to send to the LLM stored with a unique prompt id
* Completion - the completion returned from the LLM with a unique completion id and a unique prompt id
* Interaction - the interaction containing a prompt and N completions sharing a prompt id
* Chat - the chat containing a list of N interactions having the same chat id

## Testing
To test, you can use the IntelliJ HTTP client with the provided `.rest` files in the `src/test/intellij` directory.

## Todo
* remove 2 transitive vulnerabilities:
* add security
* proper parallelization in ChatService (fixed)
* add tests (fixed)
* rename objects and methods (fixed)
* get llmPrompt and llmCompletions from given llmPromptId, use LLM provider  (fixed)
* swagger (fixed)
* openAPI spec (fixed)
* remove ssl logging (fixed)
* improve error messages (partly fixed)

## Prerequisites
- Java 21 or higher
- Ollama installed and running, do: `ollama serve`, with some models
- Docker installed and running with some models
- OpenApi account, quota and key for external LLMs to use
- Anthropic account, quota and key for external LLMs to use
