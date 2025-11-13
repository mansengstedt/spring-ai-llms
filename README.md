# Chatbot client 

This service as a client, uses three different tools for chatting with different LLMs,
Ollama, Docker and OpenAI.

### Ollama
Install Ollama and download some models.

Available models are: qwen3, llama3, ...

To install and download Ollama locally with some models, see https://ollama.com/docs/installation

### Docker
Install Docker LLMs with downloaded models.

Available models are: gemma3.2, llama3.2, deepseek-r1-distill-llama, ...

To install and download Docker with some models, see https://docs.docker.com/ai/model-runner/get-started/

Specifically, add TCP support with port 12434.

### OpenAI
LLMs using OpenAI API.

Available models are: gpt-4o, gpt-4o-mini, gpt-4.1-nano, o4-mini, gpt-5, gpt-5-nano, ...
see https://platform.openai.com/docs/models

OpenApi account with quota must be opened and a key is generated for external clients to use, see https://platform.openai.com/signup,
If quota is exceeded, the service will return a 429 error code.

The OpenAI API key can be generated on the following page: https://platform.openai.com/api-keys
The generated key is read outside the app in env variable `OPEN_AI_CONNECTION_KEY`.
Ollama and Docker keys are not needed.

To see OpenApi account usage, goto https://platform.openai.com/usage

Client certificate is not used for external LLMs, but it can be configured in the application.yml file.

## Service End points

* GET /haiku create a haiku with given parameters from Docker LLMs
* POST /chat/internal chat with internal LLMs using Ollama
* POST /chat/docker chat with docker LLMs using Docker
* POST /chat/openai chat with external LLMs using OpenAI
* POST /chat/combine chat with all LLMs and combine answers
* GET /chat/request/{requestId} get request and responses from given requestId
* GET /chat/chat/{chatId} get chat by chatId
* GET /chat/status get chat service status for all LLMs


## Testing
To test, you can use the IntelliJ HTTP client with the provided `.rest` files in the `src/test/intellij` directory.

## Todo
* get request and responses from given requestId (fixed)
* rename objects and methods
* swagger
* openAPI spec
* remove ssl logging (fixed)
* improve error messages
* remove 2 transitive vulnerabilities:
* add tests

## Prerequisites
- Java 21 or higher
- Ollama installed and running, do: `ollama serve`, with some models
- Docker installed and running with some models
- OpenApi account, quota and key for external LLMs to use
