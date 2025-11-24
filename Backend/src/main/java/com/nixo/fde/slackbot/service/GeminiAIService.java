package com.nixo.fde.slackbot.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nixo.fde.slackbot.config.GeminiConfigProperties;
import com.nixo.fde.slackbot.payload.ClassificationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiAIService implements AIServiceInterface {

    private final GeminiConfigProperties geminiConfig;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public ClassificationResultDto classifyMessage(String messageText) {
        try {
            log.info("Using Gemini for classification");
            String prompt = buildClassificationPrompt(messageText);
            String response = callGemini(prompt);
            return parseClassificationResponse(response);
        } catch (Exception e) {
            log.error("Error classifying message with Gemini: {}", e.getMessage(), e);
            return ClassificationResultDto.irrelevant();
        }
    }

    @Override
    public List<Double> generateEmbedding(String text) {
        try {
            log.debug("Generating embedding with Gemini");
            return callGeminiEmbedding(text);
        } catch (Exception e) {
            log.error("Error generating embedding with Gemini: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String buildClassificationPrompt(String messageText) {
        return String.format("""
            Analyze this Slack message and determine if it's relevant to a Forward Deployed Engineer (FDE).
            
            Message: "%s"
            
            A message is RELEVANT if it's:
            - A bug report (something is broken or not working)
            - A feature request (asking for new functionality)
            - A support question (asking how to do something)
            - A product question (asking about capabilities or limitations)
            
            A message is IRRELEVANT if it's:
            - Casual conversation ("thanks", "sounds good", "let's get dinner")
            - Social messages ("good morning", "see you tomorrow")
            - Acknowledgments ("got it", "ok", "sure")
            - Off-topic discussion
            
            Respond ONLY with valid JSON in this exact format with no markdown, no code blocks, no additional text:
            {"relevant":true,"category":"BUG","title":"short summary","confidence":0.95}
            
            Categories: BUG, FEATURE_REQUEST, SUPPORT, QUESTION, NONE
            If irrelevant, use: {"relevant":false,"category":"NONE","title":null,"confidence":0.9}
            """, messageText);
    }

    private String callGemini(String prompt) throws IOException {
        String url = String.format("%s/models/%s:generateContent",
                geminiConfig.getApi().getBaseUrl(),
                geminiConfig.getModel());

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();

        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", geminiConfig.getTemperature());
        generationConfig.addProperty("maxOutputTokens", geminiConfig.getMax().getTokens());
        requestBody.add("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("x-goog-api-key", geminiConfig.getApi().getKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API call failed: " + response);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            return jsonResponse
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        }
    }

    private List<Double> callGeminiEmbedding(String text) throws IOException {
        String url = String.format("%s/models/%s:embedContent",
                geminiConfig.getApi().getBaseUrl(),
                geminiConfig.getEmbedding().getModel());

        JsonObject requestBody = new JsonObject();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();

        part.addProperty("text", text);
        parts.add(part);
        content.add("parts", parts);
        requestBody.add("content", content);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("x-goog-api-key", geminiConfig.getApi().getKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini Embedding API call failed: " + response);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            JsonArray values = jsonResponse
                    .getAsJsonObject("embedding")
                    .getAsJsonArray("values");

            List<Double> embedding = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                embedding.add(values.get(i).getAsDouble());
            }
            return embedding;
        }
    }

    private ClassificationResultDto parseClassificationResponse(String response) {
        try {
            String jsonStr = response.trim();

            // Remove markdown code blocks if present
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            } else if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }

            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }

            jsonStr = jsonStr.trim();

            JsonObject json = gson.fromJson(jsonStr, JsonObject.class);

            boolean relevant = json.get("relevant").getAsBoolean();
            String category = json.get("category").getAsString();
            String title = json.has("title") && !json.get("title").isJsonNull()
                    ? json.get("title").getAsString()
                    : null;
            double confidence = json.get("confidence").getAsDouble();

            return new ClassificationResultDto(relevant, category, title, confidence);
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            log.debug("Response was: {}", response);
            return ClassificationResultDto.irrelevant();
        }
    }
}