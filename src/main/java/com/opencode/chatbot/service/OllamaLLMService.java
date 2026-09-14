package com.opencode.chatbot.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OllamaLLMService {

    @Value("${ollama.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.timeout}")
    private long timeout;

    private final OkHttpClient httpClient;
    private final Gson gson;

    public OllamaLLMService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Send a message to the Ollama LLM and get a response
     */
    public String generateResponse(String userMessage) throws IOException {
        log.info("Sending generation request to Ollama using model {}", model);
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("prompt", userMessage);
        requestBody.addProperty("stream", false);

        Request request = new Request.Builder()
                .url(ollamaUrl + "/api/generate")
                .post(RequestBody.create(
                        gson.toJson(requestBody),
                        MediaType.get("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Ollama generation request failed with status {}", response.code());
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            String generatedResponse = jsonResponse.get("response").getAsString();
            log.info("Ollama generation completed successfully");
            return generatedResponse;
        }
    }

    /**
     * Check if Ollama service is running and accessible
     */
    public boolean isOllamaAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(ollamaUrl + "/api/tags")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                log.debug("Ollama availability request returned status {}", response.code());
                return response.isSuccessful();
            }
        } catch (IOException e) {
            log.warn("Ollama is unavailable at {}", ollamaUrl, e);
            return false;
        }
    }

    /**
     * List available models on Ollama
     */
    public String getAvailableModels() throws IOException {
        Request request = new Request.Builder()
                .url(ollamaUrl + "/api/tags")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Unable to retrieve Ollama models; status {}", response.code());
                throw new IOException("Failed to fetch models");
            }
            log.debug("Retrieved available Ollama models");
            return response.body().string();
        }
    }
}
