# Spring AI: Spring 서비스에 챗봇·RAG·MCP 도입하기

Spring AI를 활용해 기존 Spring 서비스에 **챗봇 → RAG → MCP**를 단계적으로 도입하는 실습 프로젝트

## 진행 현황

- [x] **챗봇**: OpenAI, Ollama 모델 연동
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