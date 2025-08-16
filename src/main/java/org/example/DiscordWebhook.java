package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DiscordWebhook
{

    private final HttpClient httpClient;
    private final String webhookUrl ;

    public DiscordWebhook(String webhookUrl)
    {
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public boolean sendEmbed(String title, String description, String color,
                             String author, String footer, String userId) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");

        // Add mention if provided
        if (userId != null && !userId.isEmpty()) {
            jsonBuilder.append("\"content\":\"<@").append(userId).append(">\",");
        }

        jsonBuilder.append("\"embeds\":[{");

        if (title != null && !title.isEmpty()) {
            jsonBuilder.append("\"title\":\"").append(escapeJson(title)).append("\",");
        }

        if (description != null && !description.isEmpty()) {
            jsonBuilder.append("\"description\":\"").append(escapeJson(description)).append("\",");
        }


        if (color != null && !color.isEmpty()) {
            // Convert hex color to decimal (e.g., "ff0000" -> 16711680)
            try {
                int colorInt = Integer.parseInt(color.replace("#", ""), 16);
                jsonBuilder.append("\"color\":").append(colorInt).append(",");
            } catch (NumberFormatException e) {
                // Invalid color format, skip
            }
        }

        if (author != null && !author.isEmpty()) {
            jsonBuilder.append("\"author\":{\"name\":\"").append(escapeJson(author)).append("\"},");
        }

        if (footer != null && !footer.isEmpty()) {
            jsonBuilder.append("\"footer\":{\"text\":\"").append(escapeJson(footer)).append("\"},");
        }

        // Add timestamp
        jsonBuilder.append("\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\"");
        jsonBuilder.append("}]}");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Java-Webhook-Client/1.0")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBuilder.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return response.statusCode() >= 200 && response.statusCode() < 300;

        } catch (Exception e) {
            System.err.println("Failed to send webhook embed: " + e.getMessage());
            return false;
        }
    }

    public boolean sendCheckoutSuccess(String userId, String item) {
        return sendEmbed(
                "✅ Successful Checkout!",
                "**Item:** " + item,
                "00ff00", // Green color for success
                "Volt ACO",
                "Post Success",
                userId
        );
    }

    public boolean sendCheckoutFailure(String userId, String item) {
        return sendEmbed(
                "❌ Failed Checkout!",
                "**Item:** " + item,
                "ff0000", // Red color for success
                "Volt ACO",
                "Purchase failed - check your account",
                userId
        );
    }
}
