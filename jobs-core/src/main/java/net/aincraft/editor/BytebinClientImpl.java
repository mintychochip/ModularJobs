package net.aincraft.editor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Singleton
public final class BytebinClientImpl implements BytebinClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Gson gson;

    @Inject
    public BytebinClientImpl(EditorConfig config, Gson gson) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
        this.baseUrl = config.bytebinUrl();
        this.gson = gson;
    }

    @Override
    public CompletableFuture<String> post(String json) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/post"))
            .header("Content-Type", "application/json")
            .header("User-Agent", "ModularJobs")
            .timeout(TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200 && response.statusCode() != 201) {
                    throw new BytebinException("Failed to post to bytebin: " + response.statusCode());
                }
                JsonObject obj = gson.fromJson(response.body(), JsonObject.class);
                if (!obj.has("key")) {
                    throw new BytebinException("Invalid bytebin response: missing 'key'");
                }
                return obj.get("key").getAsString();
            });
    }

    @Override
    public CompletableFuture<String> get(String code) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/" + code))
            .header("Accept", "application/json")
            .header("User-Agent", "ModularJobs")
            .timeout(TIMEOUT)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 404) {
                    throw new BytebinException("Session not found or expired");
                }
                if (response.statusCode() != 200) {
                    throw new BytebinException("Failed to get from bytebin: " + response.statusCode());
                }
                return response.body();
            });
    }

    public static final class BytebinException extends RuntimeException {
        public BytebinException(String message) {
            super(message);
        }
    }
}
