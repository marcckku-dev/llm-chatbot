# LLM Chatbot - Spring Boot

A Spring Boot application that creates a real-time chatbot with open-source LLM integration using **Ollama**.

## Features

✅ **REST API** - Send chat messages via HTTP endpoints  
✅ **WebSocket Support** - Real-time bidirectional communication  
✅ **H2 Database** - In-memory message persistence  
✅ **Ollama Integration** - Runs open-source LLMs locally (Mistral, LLaMA, etc.)  
✅ **Session Management** - Unique sessions for multiple conversations  

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **Ollama** (download from [ollama.ai](https://ollama.ai))

## Quick Start

### 1. Install and Run Ollama

```bash
# Download and install from https://ollama.ai
# Then pull a model (e.g., Mistral)
ollama pull mistral

# Start Ollama (runs on http://localhost:11434)
ollama serve
```

### 2. Build and Run the Application

```bash
# Build the project
mvn clean install

# Run the Spring Boot application
mvn spring-boot:run

# Application will be available at http://localhost:8080/api
```

## API Endpoints

### Create a New Chat Session
```http
POST /api/chat/session/new
```
**Response:**
```json
"550e8400-e29b-41d4-a716-446655440000"
```

### Send a Message (REST)
```http
POST /api/chat/message
Content-Type: application/json

{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "What is the capital of France?"
}
```

**Response:**
```json
{
  "id": 1,
  "userMessage": "What is the capital of France?",
  "aiResponse": "The capital of France is Paris...",
  "timestamp": "2024-09-14T10:30:00"
}
```

### Get Chat History
```http
GET /api/chat/history/{sessionId}
```

### Health Check
```http
GET /api/chat/health
```

## WebSocket Usage

Connect to WebSocket endpoint: `ws://localhost:8080/api/chat-socket`

**Send a message:**
```json
{
  "destination": "/app/chat",
  "headers": {},
  "body": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "Tell me a joke"
  }
}
```

**Subscribe to responses:**
```
/topic/chat/{sessionId}
```

## Database

- **Database**: H2 (in-memory)
- **Console**: http://localhost:8080/api/h2-console
  - **JDBC URL**: `jdbc:h2:mem:chatbotdb`
  - **Username**: `sa`
  - **Password**: (leave empty)

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
ollama:
  url: http://localhost:11434          # Ollama API URL
  model: mistral                         # Model name
  timeout: 30000                         # Timeout in ms
```

## Available Ollama Models

Pull any of these models:
```bash
ollama pull mistral
ollama pull llama2
ollama pull neural-chat
ollama pull orca-mini
ollama pull dolphin-mix
```

## Project Structure

```
src/main/java/com/opencode/chatbot/
├── ChatbotApplication.java         # Main Spring Boot app
├── config/
│   └── WebSocketConfig.java         # WebSocket configuration
├── controller/
│   ├── ChatController.java          # REST endpoints
│   └── ChatWebSocketController.java # WebSocket handler
├── dto/
│   ├── ChatRequest.java             # Request DTO
│   └── ChatResponse.java            # Response DTO
├── entity/
│   └── ChatMessage.java             # JPA entity
├── repository/
│   └── ChatMessageRepository.java   # Data access layer
└── service/
    ├── ChatService.java             # Business logic
    └── OllamaLLMService.java        # LLM integration
```

## Troubleshooting

### "LLM service is unavailable"
- Ensure Ollama is running: `ollama serve`
- Check Ollama URL in `application.yml`
- Verify the model is pulled: `ollama list`

### Port 8080 already in use
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

## Future Enhancements

- [ ] Add conversation context/memory to LLM
- [ ] Support for multiple LLM providers (HuggingFace, LM Studio, etc.)
- [ ] Authentication and user management
- [ ] Rate limiting
- [ ] Conversation export (PDF/JSON)
- [ ] Docker containerization

## License

MIT
