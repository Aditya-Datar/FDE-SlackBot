package com.nixo.fde.slackbot.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nixo.fde.slackbot.config.OpenAIConfigProperties;
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
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAIService implements AIServiceInterface {

    private final OpenAIConfigProperties openAiConfig;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String OPENAI_API_BASE = "https://api.openai.com/v1";

    @Override
    public ClassificationResultDto classifyMessage(String messageText) {
        try {
            log.info("Using OpenAI for classification");
            String prompt = buildClassificationPrompt(messageText);
            String response = callOpenAI(prompt);
            return parseClassificationResponse(response);
        } catch (Exception e) {
            log.error("Error classifying message with OpenAI: {}", e.getMessage(), e);
            return ClassificationResultDto.irrelevant();
        }
    }

    @Override
    public List<Double> generateEmbedding(String text) {
        try {
            log.debug("Generating embedding with OpenAI");
            return callOpenAIEmbedding(text);
        } catch (Exception e) {
            log.error("Error generating embedding with OpenAI: {}", e.getMessage(), e);
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

    private String callOpenAI(String prompt) throws IOException {
        String url = OPENAI_API_BASE + "/chat/completions";

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", openAiConfig.getModel());
        requestBody.addProperty("temperature", openAiConfig.getTemperature());
        requestBody.addProperty("max_tokens", openAiConfig.getMax().getTokens());

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        requestBody.add("messages", messages);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + openAiConfig.getApi().getKey())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API call failed: " + response);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            return jsonResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }

    private List<Double> callOpenAIEmbedding(String text) throws IOException {
        String url = OPENAI_API_BASE + "/embeddings";

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", openAiConfig.getEmbedding().getModel());
        requestBody.addProperty("input", text);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + openAiConfig.getApi().getKey())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI Embedding API call failed: " + response);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            JsonArray embeddingArray = jsonResponse
                    .getAsJsonArray("data")
                    .get(0).getAsJsonObject()
                    .getAsJsonArray("embedding");

            List<Double> embedding = new ArrayList<>();
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding.add(embeddingArray.get(i).getAsDouble());
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
            log.error("Error parsing OpenAI response: {}", e.getMessage());
            log.debug("Response was: {}", response);
            return ClassificationResultDto.irrelevant();
        }
    }
}