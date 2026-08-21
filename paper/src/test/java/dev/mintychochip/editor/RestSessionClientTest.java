package dev.mintychochip.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.mintychochip.common.editor.EditorMetadata;
import dev.mintychochip.common.editor.EditorPayload;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RestSessionClientTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void createSendsSecretAndParsesServerSession() throws Exception {
    AtomicReference<Throwable> handlerFailure = new AtomicReference<>();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/v1/sessions",
        exchange -> {
          try {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("test-secret", exchange.getRequestHeaders().getFirst("X-Create-Secret"));
            assertTrue(
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                    .contains("registeredActionTypes"));
            respond(
                exchange,
                201,
                "{\"code\":\"abc123\",\"token\":\"server-token\","
                    + "\"expiresAt\":\"2030-01-01T00:00:00Z\"}");
          } catch (AssertionError failure) {
            handlerFailure.set(failure);
            respond(exchange, 500, "{\"error\":\"handler assertion failed\"}");
          }
        });
    server.start();

    RestSessionClient.CreatedSession created = client().create(payload()).join();

    assertNull(handlerFailure.get());
    assertEquals("abc123", created.sessionCode());
    assertEquals("server-token", created.token());
    assertEquals(Instant.parse("2030-01-01T00:00:00Z"), created.expiresAt());
  }

  @Test
  void fetchSendsBothTokenHeadersAndParsesPayload() throws Exception {
    AtomicReference<Throwable> handlerFailure = new AtomicReference<>();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/v1/sessions/abc123/payload",
        exchange -> {
          try {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals(
                "Bearer server-token", exchange.getRequestHeaders().getFirst("Authorization"));
            assertEquals("server-token", exchange.getRequestHeaders().getFirst("X-Session-Token"));
            respond(exchange, 200, new Gson().toJson(payload()));
          } catch (AssertionError failure) {
            handlerFailure.set(failure);
            respond(exchange, 500, "{\"error\":\"handler assertion failed\"}");
          }
        });
    server.start();

    EditorPayload fetched = client().fetchPayload("abc123", "server-token").join();

    assertNull(handlerFailure.get());
    assertEquals(payload(), fetched);
  }

  @Test
  void classifiesExpiredAndUnauthorizedResponses() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/v1/sessions/expired/payload",
        exchange -> respond(exchange, 410, "{\"error\":\"session expired\"}"));
    server.createContext(
        "/api/v1/sessions/unauthorized/payload",
        exchange -> respond(exchange, 401, "{\"error\":\"invalid session token\"}"));
    server.start();

    RestSessionClient.RestSessionException expired =
        exceptionFrom(client().fetchPayload("expired", "token"));
    RestSessionClient.RestSessionException unauthorized =
        exceptionFrom(client().fetchPayload("unauthorized", "token"));

    assertTrue(expired.expired());
    assertFalse(unauthorized.expired());
    assertEquals(410, expired.statusCode());
    assertEquals(401, unauthorized.statusCode());
  }

  private RestSessionClient client() {
    return new RestSessionClient(
        new EditorConfig(
            true,
            "http://127.0.0.1:" + server.getAddress().getPort(),
            "https://editor.example/editor",
            "test-secret",
            60),
        new Gson());
  }

  private static EditorPayload payload() {
    return EditorPayload.create(
        EditorMetadata.create("2030-01-01T00:00:00Z", "player", "payload-token", "server"),
        Map.of(),
        List.of("modularjobs:block_break"),
        List.of("modularjobs:experience"));
  }

  private static RestSessionClient.RestSessionException exceptionFrom(
      java.util.concurrent.CompletableFuture<?> future) {
    CompletionException failure =
        org.junit.jupiter.api.Assertions.assertThrows(CompletionException.class, future::join);
    return assertInstanceOf(RestSessionClient.RestSessionException.class, failure.getCause());
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (var output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }
}
