# Spring Boot LLM Chatbot Agent Guide

## Obiettivo

Ricreare un chatbot Spring Boot con API REST, WebSocket/STOMP, persistenza H2
e integrazione locale con Ollama. L'applicazione non è una MVC server-side con
template HTML: è principalmente un'API REST con supporto WebSocket.

## Stack obbligatorio

- Java 17
- Maven 3.8+
- Spring Boot 3.2.0
- Spring Web
- Spring WebSocket
- Spring Data JPA
- H2
- Lombok
- Gson
- OkHttp 4.11.0
- Spring Boot DevTools opzionale
- Spring Boot Starter Test

## Struttura da creare

```text
src/
├── main/
│   ├── java/com/opencode/chatbot/
│   │   ├── ChatbotApplication.java
│   │   ├── config/WebSocketConfig.java
│   │   ├── controller/ChatController.java
│   │   ├── controller/ChatWebSocketController.java
│   │   ├── dto/ChatRequest.java
│   │   ├── dto/ChatResponse.java
│   │   ├── entity/ChatMessage.java
│   │   ├── repository/ChatMessageRepository.java
│   │   └── service/
│   │       ├── ChatService.java
│   │       └── OllamaLLMService.java
│   └── resources/application.yml
└── test/
    ├── java/com/opencode/chatbot/
    │   ├── ChatbotIntegrationTest.java
    │   ├── controller/ChatControllerTest.java
    │   ├── repository/ChatMessageRepositoryTest.java
    │   └── service/ChatServiceTest.java
    └── resources/application.yml
```

## Ordine di implementazione

1. Creare il progetto Maven con Java 17 e le dipendenze indicate.
2. Creare `ChatbotApplication` con `@SpringBootApplication`.
3. Configurare H2 in-memory e il context path `/api`.
4. Creare l'entità `ChatMessage`.
5. Creare il repository con:
   `findBySessionIdOrderByTimestampAsc(String sessionId)`.
6. Implementare `OllamaLLMService`.
7. Implementare `ChatService`.
8. Esporre il controller REST.
9. Configurare WebSocket/STOMP.
10. Aggiungere logging con Lombok `@Slf4j`.
11. Aggiungere test unitari, repository e integrazione.
12. Eseguire `mvn clean install`.

## Configurazione applicativa

In `src/main/resources/application.yml` usare:

```yaml
spring:
  application:
    name: llm-chatbot
  datasource:
    url: jdbc:h2:mem:chatbotdb
    driverClassName: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  h2:
    console:
      enabled: true
      path: /h2-console

server:
  port: 8581
  servlet:
    context-path: /api

logging:
  file:
    name: logs/llm-chatbot.log
  level:
    com.opencode.chatbot: INFO

ollama:
  url: http://localhost:11434
  model: mistral
  timeout: 30000
```

## Modello dati

`ChatMessage` deve contenere:

- `id`: `Long`, generato con `GenerationType.IDENTITY`;
- `sessionId`: obbligatorio;
- `userMessage`: obbligatorio, campo database `TEXT`;
- `aiResponse`: campo database `TEXT`;
- `timestamp`: obbligatorio, `LocalDateTime`.

Impostare il timestamp automaticamente con `@PrePersist`.

## Integrazione Ollama

Installare Ollama su Windows:

```powershell
irm https://ollama.com/install.ps1 | iex
```

La versione verificata è `0.34.0`. Avviare il servizio e scaricare il modello:

```powershell
ollama serve
ollama pull mistral
ollama list
```

Il modello atteso è `mistral:latest`. L'API deve rispondere su
`http://localhost:11434`.

`OllamaLLMService` deve:

- chiamare `GET /api/tags` per il health check;
- chiamare `POST /api/generate` con `model`, `prompt` e `stream: false`;
- restituire il campo JSON `response`;
- propagare gli errori HTTP come `IOException`;
- usare timeout di connessione di 30 secondi e lettura di 120 secondi.

Ollama esegue il modello localmente e non garantisce risposte corrette,
aggiornate o prive di allucinazioni. Non ha automaticamente accesso al
database, ai file locali o a Internet. L'applicazione invia al modello solo il
messaggio corrente e non implementa memoria conversazionale.

## API REST

Base URL: `http://localhost:8581/api`.

### Creazione sessione

```http
POST /chat/session/new
```

Restituisce un UUID in formato stringa.

### Invio messaggio

```http
POST /chat/message
Content-Type: application/json
```

Body:

```json
{
  "sessionId": "session-id",
  "message": "Ciao"
}
```

Il servizio genera la risposta con Ollama, salva il record e restituisce
`id`, `userMessage`, `aiResponse` e `timestamp`. In caso di `IOException`
restituisce HTTP `500` e non salva il messaggio.

### Cronologia

```http
GET /chat/history/{sessionId}
```

Restituisce i messaggi della sessione ordinati per timestamp crescente.

### Health check

```http
GET /chat/health
```

Restituisce HTTP `200` se Ollama è disponibile e HTTP `503` altrimenti.

## WebSocket/STOMP

Configurare:

- endpoint SockJS: `ws://localhost:8581/api/chat-socket`;
- prefisso applicazione: `/app`;
- broker: `/topic`;
- destinazione messaggi: `/app/chat`;
- destinazione risposte: `/topic/chat/{sessionId}`.

`ChatWebSocketController` deve elaborare lo stesso flusso del controller REST.
In caso di errore invia una `ChatResponse` con messaggio `Error: ...`.

Usare l'import corretto:

```java
org.springframework.web.socket.config.annotation.StompEndpointRegistry
```

## Logging

Usare Lombok `@Slf4j` nei service e controller. Registrare:

- ricezione delle richieste;
- creazione sessioni;
- chiamate e codici HTTP Ollama;
- persistenza e recupero cronologia;
- disponibilità del servizio;
- errori REST, WebSocket e Ollama con stack trace.

Non scrivere nei log il testo completo dei prompt o delle risposte AI.
In esecuzione reale i log vanno in `logs/llm-chatbot.log`; i file `.log` devono
essere esclusi da Git.

## Test

Creare:

- test Mockito per `ChatService`;
- `@WebMvcTest(ChatController.class)` per gli endpoint REST;
- `@DataJpaTest` per repository, ID, timestamp e ordinamento;
- `@SpringBootTest` con `@AutoConfigureMockMvc` per il flusso REST completo.

Nei test di integrazione mockare `OllamaLLMService`, così la suite non dipende
da Ollama reale. Verificare sia il caso positivo sia l'errore senza persistenza.

Per evitare gli stack trace applicativi simulati nella console durante i test,
creare `src/test/resources/application.yml`:

```yaml
logging:
  level:
    com.opencode.chatbot: OFF
```

Questo vale solo per i test. In esecuzione normale gli errori restano loggati.

## Verifica finale

Con Java 17 eseguire:

```powershell
mvn clean install
```

Il risultato atteso è:

```text
Tests run: 13
Failures: 0
Errors: 0
BUILD SUCCESS
```

Verifiche manuali:

```powershell
Invoke-WebRequest http://localhost:11434/api/tags
Invoke-WebRequest http://localhost:8581/api/chat/health
```

L'health check dell'applicazione deve rispondere:

```text
LLM service is available
```

## File da non versionare

Il `.gitignore` deve escludere almeno:

```text
target/
logs/
.idea/
.vscode/
*.iml
*.log
.env
```

Versionare invece `pom.xml`, `src/`, `README.md`,
`Comportamento-App-Info.md` e questo file agente.