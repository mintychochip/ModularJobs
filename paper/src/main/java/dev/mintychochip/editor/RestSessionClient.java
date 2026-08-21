package dev.mintychochip.editor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletableFuture;
import dev.mintychochip.common.editor.EditorPayload;

/**
 * HTTP client for the Rust-backed editor session API.
 */
public final class RestSessionClient {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private final HttpClient httpClient;
  private final String baseUrl;
  private final String createSecret;
  private final Gson gson;

  public RestSessionClient(EditorConfig config, Gson gson) {
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .build();
    this.baseUrl = trimTrailingSlashes(config.sessionApiUrl());
    this.createSecret = config.sessionCreateSecret();
    this.gson = gson;
  }

  public CompletableFuture<CreatedSession> create(EditorPayload payload) {
    HttpRequest.Builder builder = requestBuilder("/api/v1/sessions")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8));
    if (!createSecret.isBlank()) {
      builder.header("X-Create-Secret", createSecret);
    }
    return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          requireStatus(response, 200, 201);
          JsonObject body = parseObject(response, "create session response");
          String code = requiredString(body, "code", response.statusCode());
          String token = requiredString(body, "token", response.statusCode());
          String expiresAt = requiredString(body, "expiresAt", response.statusCode());
          return new CreatedSession(code, token, parseExpiry(expiresAt, response.statusCode()));
        });
  }

  public CompletableFuture<EditorPayload> fetchPayload(String sessionCode, String token) {
    if (sessionCode == null || sessionCode.isBlank()) {
      throw new IllegalArgumentException("session code is required");
    }
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("session token is required");
    }

    String path = "/api/v1/sessions/" + encodePathSegment(sessionCode) + "/payload";
    HttpRequest request = requestBuilder(path)
        .header("Authorization", "Bearer " + token)
        .header("X-Session-Token", token)
        .GET()
        .build();
    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          requireStatus(response, 200);
          try {
            EditorPayload payload = gson.fromJson(response.body(), EditorPayload.class);
            if (payload == null) {
              throw new JsonParseException("response body is null");
            }
            return payload;
          } catch (JsonParseException e) {
            throw new RestSessionException(
                response.statusCode(),
                "invalid session payload: " + e.getMessage(),
                false,
                e);
          }
        });
  }

  private HttpRequest.Builder requestBuilder(String path) {
    return HttpRequest.newBuilder(URI.create(baseUrl + path))
        .timeout(TIMEOUT);
  }

  private static void requireStatus(HttpResponse<String> response, int... accepted) {
    for (int status : accepted) {
      if (response.statusCode() == status) {
        return;
      }
    }
    int status = response.statusCode();
    throw new RestSessionException(
        status,
        responseMessage(response),
        status == 410,
        null);
  }

  private static JsonObject parseObject(HttpResponse<String> response, String description) {
    try {
      JsonObject object = new Gson().fromJson(response.body(), JsonObject.class);
      if (object == null) {
        throw new JsonParseException("response body is null");
      }
      return object;
    } catch (JsonParseException e) {
      throw new RestSessionException(
          response.statusCode(),
          "invalid " + description + ": " + e.getMessage(),
          false,
          e);
    }
  }

  private static String requiredString(JsonObject body, String field, int status) {
    if (!body.has(field) || body.get(field).isJsonNull() || !body.get(field).isJsonPrimitive()) {
      throw new RestSessionException(status, "missing response field: " + field, false, null);
    }
    String value = body.get(field).getAsString();
    if (value.isBlank()) {
      throw new RestSessionException(status, "empty response field: " + field, false, null);
    }
    return value;
  }

  private static Instant parseExpiry(String value, int status) {
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException e) {
      throw new RestSessionException(status, "invalid expiresAt: " + value, false, e);
    }
  }

  private static String responseMessage(HttpResponse<String> response) {
    String body = response.body();
    if (body == null || body.isBlank()) {
      return "REST session request failed with HTTP " + response.statusCode();
    }
    try {
      JsonObject object = new Gson().fromJson(body, JsonObject.class);
      if (object != null && object.has("error") && object.get("error").isJsonPrimitive()) {
        return object.get("error").getAsString();
      }
    } catch (JsonParseException ignored) {
      // Fall through to the bounded raw response message.
    }
    return "REST session request failed with HTTP " + response.statusCode() + ": " + body;
  }

  private static String trimTrailingSlashes(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("session API URL is required");
    }
    return value.trim().replaceFirst("/+$", "");
  }

  private static String encodePathSegment(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  public record CreatedSession(String sessionCode, String token, Instant expiresAt) {
  }

  public static final class RestSessionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final boolean expired;

    private RestSessionException(int statusCode, String message, boolean expired, Throwable cause) {
      super(message, cause);
      this.statusCode = statusCode;
      this.expired = expired;
    }

    public int statusCode() {
      return statusCode;
    }

    public boolean expired() {
      return expired;
    }
  }
}
