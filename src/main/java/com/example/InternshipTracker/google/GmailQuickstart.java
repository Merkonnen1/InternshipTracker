// Java
package com.example.InternshipTracker.google;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class GmailQuickstart {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final com.google.api.client.json.JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // Only read emails
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/gmail.readonly");

    /**
     * Fetches the latest email body (text/plain if available; fallback to text/html stripped) from the user's mailbox.
     */
    public static String fetchLatestEmailPlainText(Gmail gmail) throws Exception {
        var list = gmail.users().messages().list("me")
                .setMaxResults(1L)
                .setQ("newer_than:14d category:primary")
                .execute();

        List<Message> messages = list.getMessages();
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        String messageId = messages.get(0).getId();
        var msg = gmail.users().messages().get("me", messageId).setFormat("FULL").execute();

        var payload = msg.getPayload();
        if (payload == null) return "";

        // Try to find text/plain part
        String text = findPart(payload, "text/plain")
                .or(() -> findPart(payload, "text/html").map(GmailQuickstart::stripHtml))
                .orElse("");

        return text;
    }

    private static Optional<String> findPart(com.google.api.services.gmail.model.MessagePart part, String mimeType) {
        if (part.getMimeType() != null && part.getMimeType().startsWith(mimeType) && part.getBody() != null && part.getBody().getData() != null) {
            return Optional.of(decodeBase64Url(part.getBody().getData()));
        }
        if (part.getParts() != null) {
            for (var p : part.getParts()) {
                var found = findPart(p, mimeType);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    private static String stripHtml(String html) {
        return html.replaceAll("(?s)<style.*?</style>", " ")
                .replaceAll("(?s)<script.*?</script>", " ")
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String decodeBase64Url(String data) {
        byte[] decoded = Base64.getUrlDecoder().decode(data);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * Calls an AI endpoint to extract job info from an email body.
     * Expects the response content to be valid JSON with fields like: company, position, status.
     */
    public static JsonObject extractJobInfoWithAI(String emailContent, String apiKey) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing API key for AI extraction. Set OPENAI_API_KEY.");
        }
        String prompt = """
            Extract job application information as a compact JSON object with keys: company, position, status, contact, important_dates.
            If not present, use empty strings. Only return valid JSON, no extra text.
            Email:
            """ + emailContent;

        JsonObject body = new JsonObject();
        body.addProperty("model", "gpt-4o-mini");
        var messages = new com.google.gson.JsonArray();
        var sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", "You extract structured job application data. Respond ONLY with a JSON object.");
        var usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", prompt);
        messages.add(sys);
        messages.add(usr);
        body.add("messages", messages);
        // Ask for JSON object output
        var responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        body.add("response_format", responseFormat);

        HttpClient http = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("AI API error: HTTP " + resp.statusCode() + " - " + resp.body());
        }

        var json = JsonParser.parseString(resp.body()).getAsJsonObject();
        var choices = json.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("AI API returned no choices");
        }
        var content = choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        return JsonParser.parseString(content).getAsJsonObject();
    }
}