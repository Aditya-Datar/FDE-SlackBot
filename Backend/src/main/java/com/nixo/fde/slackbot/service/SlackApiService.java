package com.nixo.fde.slackbot.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackApiService {

    @Value("${slack.bot.token}")
    private String botToken;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    // Cache to avoid repeated API calls for same user
    private final Map<String, String> userNameCache = new ConcurrentHashMap<>();

    /**
     * Get user's real name from Slack API
     * @param userId Slack user ID (e.g., U09UF5MHLAX)
     * @return Real name or fallback to "Customer"
     */
    public String getUserRealName(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "Customer";
        }

        // Check cache first
        if (userNameCache.containsKey(userId)) {
            return userNameCache.get(userId);
        }

        try {
            String url = "https://slack.com/api/users.info?user=" + userId;

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + botToken)
                    .header("Content-Type", "application/json")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Slack API call failed for user {}: {}", userId, response);
                    return "Customer";
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.get("ok").getAsBoolean()) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");

                    // Try to get real_name, fallback to name
                    String realName = null;
                    if (user.has("real_name") && !user.get("real_name").isJsonNull()) {
                        realName = user.get("real_name").getAsString();
                    } else if (user.has("profile")) {
                        JsonObject profile = user.getAsJsonObject("profile");
                        if (profile.has("real_name") && !profile.get("real_name").isJsonNull()) {
                            realName = profile.get("real_name").getAsString();
                        } else if (profile.has("display_name") && !profile.get("display_name").isJsonNull()) {
                            realName = profile.get("display_name").getAsString();
                        }
                    }

                    if (realName == null || realName.isEmpty()) {
                        realName = user.has("name") ? user.get("name").getAsString() : "Customer";
                    }

                    // Cache the result
                    userNameCache.put(userId, realName);
                    log.info("Resolved user {} to name: {}", userId, realName);
                    return realName;
                } else {
                    log.warn("Slack API returned error for user {}: {}", userId, jsonResponse.get("error"));
                    return "Customer";
                }
            }
        } catch (IOException e) {
            log.error("Error fetching user info from Slack for user {}: {}", userId, e.getMessage());
            return "Customer";
        }
    }

    /**
     * Get channel name from Slack API
     * @param channelId Slack channel ID (e.g., C09VDMGLJ5P)
     * @return Channel name or the ID if not found
     */
    public String getChannelName(String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return "unknown";
        }

        try {
            String url = "https://slack.com/api/conversations.info?channel=" + channelId;

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + botToken)
                    .header("Content-Type", "application/json")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return channelId;
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.get("ok").getAsBoolean()) {
                    JsonObject channel = jsonResponse.getAsJsonObject("channel");
                    String channelName = channel.get("name").getAsString();
                    return "#" + channelName;
                }
            }
        } catch (Exception e) {
            log.error("Error fetching channel info: {}", e.getMessage());
        }

        return channelId;
    }

    /**
     * Clear user name cache (useful for testing or when users change names)
     */
    public void clearCache() {
        userNameCache.clear();
        log.info("User name cache cleared");
    }
}