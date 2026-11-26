# Spring AI: Spring 서비스에 챗봇·RAG·MCP 도입하기

Spring AI를 활용해 기존 Spring 서비스에 **챗봇 → RAG → MCP**를 단계적으로 도입하는 실습 프로젝트

## 진행 현황

- [x] **챗봇**: OpenAI, Ollama 모델 연동
  - [x] `content()`로 텍스트 응답 받기
  - [x] `ChatResponse`로 메타데이터 포함 응답 받기
  - [x] `stream()`으로 스트리밍 응답 받기
- [ ] **RAG**: 문서 기반 검색 증강 생성
- [ ] **MCP**: Model Context Protocol 연동

## 기술 스택

- Java / Spring Boot
- Spring AI
- Gradle
- LLM
  - OpenAI: `gpt-4o-mini`
  - Ollama: `Qwen2.5-1.5B-Instruct` (GGUF)

## 프로젝트 구조

```
src/main/java/com/jscode/chat
├── ChatApplication.java
└── controller
    └── ChatController.java
```

## 실행 방법

### 1. OpenAI API 키 설정

API 키는 코드에 직접 작성하지 않고 환경변수로 주입

```bash
export OPENAI_API_KEY=sk-...
```

IntelliJ 실행 시 **Run Configuration → Environment variables**에 `OPENAI_API_KEY` 추가

### 2. Ollama 실행 (로컬 모델 사용 시)

[Ollama](https://ollama.com) 설치 후 실행

```bash
ollama serve
```

`pull-model-strategy: when_missing` 설정으로 모델이 없을 경우 애플리케이션 시작 시 자동 다운로드

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

## 설정 (`application.yaml`)

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4o-mini
    ollama:
      init:
        pull-model-strategy: when_missing
      chat:
        model: hf.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF
```

## 응답 방식

`ChatClient`는 응답을 받는 방식에 따라 결과 타입이 달라진다.

| 호출 방식 | 반환 타입 | 특징 |
| --- | --- | --- |
| `.call().content()` | `String` | 완성된 답변 텍스트만 반환 |
| `.call().chatResponse()` | `ChatResponse` | 답변 + 메타데이터(모델명, 토큰 사용량 등) 반환 |
| `.stream().content()` | `Flux<String>` | 생성되는 대로 조각(토큰) 단위로 반환 |

### 1. content(): 텍스트 응답

`/ai`

```java
@GetMapping("/ai")
public String generation(String userPrompt) {
    return this.chatClient.prompt()
            .user(userPrompt)   // 사용자 메시지 전달
            .call()             // 모델 호출 (답변이 끝까지 생성될 때까지 대기)
            .content();         // 답변 텍스트만 추출
}
```

- 가장 기본적인 호출 방식으로, 모델의 답변을 **단순한 `String`**으로 반환
- 답변이 완성된 뒤 한 번에 전달되며, 메타데이터는 포함되지 않음
- 답변 텍스트만 필요할 때 사용

### 2. ChatResponse: 메타데이터 포함 응답

`/cr`

```java
@GetMapping(value = "/cr", produces = MediaType.APPLICATION_JSON_VALUE)
public @Nullable ChatResponse chatResponse(String userPrompt) {
    return this.chatClient.prompt()
            .user(userPrompt)
            .call()
            .chatResponse();
}
```

- `.content()`가 답변 문자열만 주는 것과 달리, `ChatResponse`는 답변과 함께 **메타데이터**를 담고 있음
  - `result.output`: 모델이 생성한 답변 메시지
  - `metadata`: 사용한 모델명, 토큰 사용량(`promptTokens`, `completionTokens`, `totalTokens`) 등
- 토큰 사용량 모니터링, 비용 계산, 로깅 등에 활용
- `APPLICATION_JSON_VALUE`로 응답하므로 객체가 JSON으로 직렬화되어 전달됨

### 3. Stream: 스트리밍 응답

`/st`

```java
@GetMapping(value = "/st", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(String userPrompt) {
    return this.chatClient.prompt()
            .user(userPrompt)
            .stream()
            .content();
}
```

- `call()`은 답변이 끝까지 생성된 뒤 한 번에 반환하지만, `stream()`은 **생성되는 대로 조각(토큰) 단위로** 반환
  - 예: `"스프링"` → `" AI는"` → `" 자바"` → `" 기반의"` ...
- 반환 타입 `Flux<T>`: Project Reactor의 타입으로, 0개 이상의 데이터가 시간에 걸쳐 비동기로 도착하는 흐름 (단일 값은 `Mono<T>`)
- `TEXT_EVENT_STREAM_VALUE`(SSE, Server-Sent Events)로 응답하면 연결을 유지한 채 조각이 도착할 때마다 클라이언트에 바로 전달됨
- 첫 글자가 뜨는 시간이 짧아져 채팅 UI의 체감 속도가 좋아짐
- 메타데이터까지 스트림으로 받으려면 `.content()` 대신 `.chatResponse()` 사용 → `Flux<ChatResponse>`

#### call() vs stream() 언제 쓰나?

- **stream()**: 사용자가 답변을 기다리는 채팅 화면
- **call()**: 완성된 결과가 있어야 다음 단계로 넘어갈 수 있는 작업 (JSON 파싱, DB 저장 등)
