package net.aincraft.editor;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for interacting with a bytebin paste service.
 * Used to store and retrieve JSON payloads for the web editor.
 */
public interface BytebinClient {

    /**
     * Posts JSON content to bytebin.
     *
     * @param json the JSON string to upload
     * @return a future containing the paste code/key
     */
    CompletableFuture<String> post(String json);

    /**
     * Retrieves JSON content from bytebin by code.
     *
     * @param code the paste code/key
     * @return a future containing the JSON string
     */
    CompletableFuture<String> get(String code);
}
