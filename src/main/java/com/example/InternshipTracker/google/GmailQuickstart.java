package com.example.InternshipTracker.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.gson.*;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.Base64;

public class GmailQuickstart {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // Directory where OAuth tokens are stored locally
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    // Only read emails
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);

    // Expect credentials.json on the classpath (e.g., src/main/resources/credentials.json)
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Calls an AI endpoint to extract job info from an email body.
     * Expects the response content to be valid JSON with fields like: company, position, status.
     */
    public static JsonObject extractJobInfoWithAI(String emailContent, String apiKey) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing API key for AI extraction. Set OPENAI_API_KEY or pass a non-empty key.");
        }

        // Provide a short instruction to respond with a compact JSON object only.
        String instruction = "Extract internship or job tracking info from the email. " +
                "Return a compact JSON object with fields: company (string), position (string), status (string either Pending,Under Review,Accepted,Rejected or null if not internship or job), " +
                "deadline (string in format yyyy-MM-dd or null). If uncertain, use null.";

        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", instruction);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", emailContent == null ? "" : emailContent);
        messages.add(userMsg);

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "gpt-4o-mini");
        payload.add("messages", messages);
        payload.addProperty("temperature", 0);

        String requestBody = new Gson().toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            throw new IOException("AI API call failed with status " + response.statusCode() + ": " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new IllegalStateException("AI response did not contain choices.");
        }

        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            throw new IllegalStateException("AI response missing message content.");
        }

        String content = message.get("content").getAsString();
        // Expect JSON content. If it's not strictly JSON, try to find a JSON object in the text.
        try {
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception ignore) {
            // Best-effort fallback: attempt to extract the first JSON object substring.
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String candidate = content.substring(start, end + 1);
                return JsonParser.parseString(candidate).getAsJsonObject();
            }
            throw new IllegalStateException("AI response content was not valid JSON: " + content);
        }
    }

    private static Credential getCredentials(final HttpTransport httpTransport) throws IOException {
        InputStream in = GmailQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("credentials.json not found on classpath at: " + CREDENTIALS_FILE_PATH);
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);

            // Store tokens under user home to persist across runs
            File tokensDir = new File(System.getProperty("user.home"), ".gmail/" + TOKENS_DIRECTORY_PATH);
            if (!tokensDir.exists() && !tokensDir.mkdirs()) {
                throw new IOException("Failed to create tokens directory: " + tokensDir.getAbsolutePath());
            }

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }


    private static String getHeader(List<MessagePartHeader> headers, String name) {
        if (headers == null) return "";
        for (MessagePartHeader h : headers) {
            if (name.equalsIgnoreCase(h.getName())) {
                return h.getValue();
            }
        }
        return "";
    }

    private static String getPlainTextFromMessage(Message message) {
        if (message == null) return "";
        MessagePart payload = message.getPayload();
        if (payload == null) return "";
        // Prefer text/plain, fall back to text/html (stripped)
        String text = getTextFromPart(payload, true);
        if (text.isBlank()) {
            text = getTextFromPart(payload, false);
        }
        return text.trim();
    }

    /**
     * Recursively extract text from a message part.
     * @param preferPlain if true, prefers text/plain; if false, returns text/html (stripped) if plain not found.
     */
    private static String getTextFromPart(MessagePart part, boolean preferPlain) {
        if (part == null) return "";

        String mime = Optional.ofNullable(part.getMimeType()).orElse("").toLowerCase(Locale.ROOT);
        String data = decodeBody(part.getBody());

        if (preferPlain) {
            if (mime.startsWith("text/plain")) {
                return data;
            }
        } else {
            if (mime.startsWith("text/html")) {
                return stripHtml(data);
            }
        }

        if (part.getParts() != null && !part.getParts().isEmpty()) {
            String best = "";
            for (MessagePart sub : part.getParts()) {
                String candidate = getTextFromPart(sub, preferPlain);
                if (!candidate.isBlank()) {
                    // Return first non-blank match in the preferred type
                    return candidate;
                }
                if (best.isBlank()) {
                    best = candidate;
                }
            }
            return best;
        }

        // If it's single-part but not the preferred type, optional fallback
        if (!preferPlain && mime.startsWith("text/plain")) {
            return data;
        }
        if (preferPlain && mime.startsWith("text/html")) {
            return stripHtml(data);
        }

        return "";
    }

    private static String decodeBody(MessagePartBody body) {
        if (body == null || body.getData() == null) return "";
        String raw = body.getData();
        try {
            int padding = (4 - (raw.length() % 4)) % 4;
            String padded = raw + "=".repeat(padding);
            byte[] decoded = Base64.getUrlDecoder().decode(padded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Fallback: try standard Base64 after URL char replacements
            String fixed = raw.replace('-', '+').replace('_', '/');
            int padding2 = (4 - (fixed.length() % 4)) % 4;
            fixed = fixed + "=".repeat(padding2);
            try {
                byte[] decoded = Base64.getDecoder().decode(fixed);
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (Exception ignore) {
                return "";
            }
        }
    }

    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        // Minimal HTML tag stripping to keep dependencies low.
        // For better results, consider an HTML parser.
        String text = html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?s)<br\\s*/?>", "\n")
                .replaceAll("(?s)</p>", "\n")
                .replaceAll("(?s)<[^>]+>", " ");
        // Collapse whitespace
        return text.replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }
    public List<JsonObject> fetchEmailsWithAi() throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        Gmail service = new Gmail.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Fetch the 10 most recent messages from the last 30 days (optional query)
        ListMessagesResponse listResponse = service.users()
                .messages()
                .list("me")
                .setMaxResults(10L)
                .setQ("newer_than:30d")
                .execute();

        List<Message> messages = listResponse.getMessages();
        if (messages == null || messages.isEmpty()) {
            System.out.println("No messages found.");
                return null;
        }

        String apiKey = "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";
        List<JsonObject> ans = new ArrayList<>();
        for (Message m : messages) {
            Message full = service.users().messages().get("me", m.getId()).setFormat("full").execute();

            MessagePart payload = full.getPayload();
            List<MessagePartHeader> headers = payload != null ? payload.getHeaders() : Collections.emptyList();

            String from = getHeader(headers, "From");
            String subject = getHeader(headers, "Subject");
            String date = getHeader(headers, "Date");

            String body = getPlainTextFromMessage(full);
            if (!apiKey.isBlank() && body != null && !body.isBlank()) {
                try {
                    JsonObject extracted = extractJobInfoWithAI(body, apiKey);
                    ans.add(extracted);
                } catch (Exception e) {
                    System.err.println("AI extraction failed: " + e.getMessage());
                }
            }
        }
        return ans;
    }

}
