# Chatbot client 

The service, acting as a client, uses many different tools for chatting with different LLMs like
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

OpenApi account with quota must be opened, a key is generated for external clients to use, see https://platform.openai.com/signup,
If quota is exceeded/unavailable, the service will return a 429 error code.

The OpenAI API key can be generated on the following page: https://platform.openai.com/api-keys
The generated key is read outside the app in env variable `OPEN_AI_CONNECTION_KEY`.

To see OpenApi account usage, go to https://platform.openai.com/usage

Client certificate is not used for external LLMs, but it can be configured in the application.yml file.

### Anthropic
LLMs using Anthropic API.

Available models are: claude-sonnet-4-20250514, claude-sonnet-4-5-20250929, ...
see https://www.anthropic.com/claude/sonnet

Anthropic account with quota must be opened, a key is generated for external clients to use, 
see https://console.anthropic.com/docs/en/home or https://console.anthropic.com/settings/organization.
If quota is exceeded/unavailable, the service will return a 429 error code.

The Anthropic API key can be generated on the following page: https://console.anthropic.com/settings/keys
The generated key is read outside the app in env variable `ANTHROPIC_CONNECTION_KEY`.

To see OpenApi account usage, go to https://console.anthropic.com/settings/billing or https://console.anthropic.com/usage

### Google Gemini
LLM using Google Gemini API.

Steps to install and use Gemini/Vertex:

- Create a project in Google Cloud Platform.
- Create a service account (here name: `ment-chat`, project: `lithe-breaker-480809-c9`).

- Create a role as below to avoid permission problems
- Go to the IAM & Admin Console: Navigate to the Service Accounts page in your GCP project.
- Select the Target Service Account: Find and click on the Service Account specified in the URL: `ment-chat@lithe-breaker-480809-c9.iam.gserviceaccount.com`.
- Add a Principal: On the Service Account details page, look for the Permissions tab and click Grant Access (or ADD PRINCIPAL).
- Specify the Principal (The Caller, in this case `mans.engstedt@gmail.com`): 
- In the New principals field, enter the email address of the Caller (the entity that is currently experiencing the 403 error).
- Select the Role: In the Select a role dropdown, choose: Service Account Token Creator.

- Download gcloud CLI and run: gcloud auth application-default login --impersonate-service-account ment-chat@lithe-breaker-480809-c9.iam.gserviceaccount.com
- Now the credentials file is saved in `~/.config/gcloud/application_default_credentials.json`.
- Set the environment variable `GOOGLE_APPLICATION_CREDENTIALS` to the path of the credentials file.
- in application.yaml, set `spring.ai.vertex.ai.gemini.location` to `europe-north1` and `spring.ai.vertex.ai.gemini.project-id` to `lithe-breaker-480809-c9`.

Location and projectId are fetched from properties in `spring.ai.vertex.ai.gemini`. 

The model name is fetched from `app.gemini.llm-model.name`, but defaults to value in `LlmConfig.name`.

Do not use the model name values in `org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel.ChatModel`, like `GEMINI_2_5_PRO`,
when setting the model name in configuring the VertexAiGeminiChatModel, since the corresponding names are outdated.
Working example values are `gemini-2-5-pro`, `gemini-2-5-flash` (multimodal), `gemini-3-pro-preview` and `gemini-3-pro`.

However, the model returned by gemini is empty. In the code it is set to the configured value.

- Gemini example parameters: https://docs.cloud.google.com/vertex-ai/generative-ai/docs/models/gemini/2-5-pro
- Available foundation models: https://console.cloud.google.com/vertex-ai/model-garden
- Service accounts: https://console.cloud.google.com/projectselector2/iam-admin/serviceaccounts?supportedpurview=project
- See/create Api key: https://console.cloud.google.com/api-sandbox/api-keys?project=curious-crow-jmvrn

Example Projects
- Josh Long: https://cloud.google.com/blog/topics/developers-practitioners/google-cloud-and-spring-ai-10
- google: https://console.cloud.google.com/welcome/new?project=lithe-breaker-480809-c9&walkthrough_id=vertex-ai--prompt_design
- ChatMemory: https://www.youtube.com/watch?v=QTaCb7lxyL8

## Service End points

* POST /chat/provider/haiku?provider={provider} create a haiku from provider, for example Anthropic, with given parameters
* POST /chat/provider/prompt chat with a given LLM in the request object, one of OLLAMA, DOCKER, OPENAI, ANTHROPIC or GEMINI
* POST /chat/providers/prompt chat with given LLMs in the request object, in set OLLAMA, DOCKER, OPENAI, ANTHROPIC, GEMINI
* DELETE /chat/history/clear?chat_id={chat_id}&provider={provider} clear chat history for a Provider
* GET /chat/history?chat_id={chat_id}&provider={provider} get chat history for a Provider
* GET /chat/chat/{chat-id} get chat by chatId
* GET /chat/prompt/{prompt-id} get llmPrompt and completions from the given promptId
* GET /chat/prompt/contains/{part-of-prompt} get chats containing the given part of the prompt
* GET /chat/completion/{completion-id} get chats containing the given completionId
* GET /chat/completion/contains/{part-of-completion} get chats containing the given part of the completion
* GET /chat/provider/status get chat service status for all LLMs

See http://localhost:8999/swagger-ui/index.html for swagger documentation.


## Model
The main domain objects are:
* Prompt - the prompt to send to the LLM stored with a unique prompt id, having a set of completions
* Completion - the completion returned from the LLM with a unique completion id and a unique prompt id
* Interaction - the interaction containing a prompt and N completions sharing a prompt id
* Chat - the chat containing a list of N interactions having the same chat id

## Testing
To test, you can use the IntelliJ HTTP client with the provided `.rest` files in the `src/test/intellij` directory.

## Todo
* Aggregate endpoint added (fixed)
* ChatMemory, endpoint to show history and clear memory, reference to ChatMemory needed for each client (fixed)
* Dynamic system in REST API (fixed) 
* Message.ASSISTANT should not be used (fixed)
* Session handling to separate different chats after restart (fixed)
* Tooling (fixed)
* choose which provider to use: replace 'llm/all' endpoint with chosen providers (fixed)
* upgrade spring-ai to the latest version 1.1.2 (fixed)
* add Gemini (fixed)
* remove 2 transitive vulnerabilities (fixed)
* add security (later)
* proper parallelization in ChatService (fixed)
* add tests (fixed)
* rename objects and methods (fixed)
* get llmPrompt and llmCompletions from given llmPromptId, use LLM provider  (fixed)
* swagger (fixed)
* openAPI spec (fixed)
* remove ssl logging (fixed)
* improve error messages (partly fixed)

## Prerequisites
- Java 24 or higher
- Ollama installed and running, do: `ollama serve`, with some models
- Docker installed and running with some models
- OpenApi account, quota and key for external LLMs to use
- Anthropic account, quota and key for external LLMs to use
- Gemini account, project, role, quota and key for external LLMs to use
