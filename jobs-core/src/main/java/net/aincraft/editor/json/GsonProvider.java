package net.aincraft.editor.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import net.kyori.adventure.key.Key;
import java.io.IOException;
import java.time.Instant;

/**
 * Guice provider that creates a configured Gson instance for the ModularJobs web editor.
 * <p>
 * Configuration:
 * <ul>
 *   <li>Pretty printing enabled for readability</li>
 *   <li>Lenient parsing enabled</li>
 *   <li>Null values are NOT serialized (default behavior)</li>
 * </ul>
 * <p>
 * Custom type adapters:
 * <ul>
 *   <li>{@link Key} - serialized as "namespace:value" strings</li>
 *   <li>{@link Instant} - serialized as ISO-8601 strings</li>
 * </ul>
 */
@Singleton
public final class GsonProvider implements Provider<Gson> {

    @Override
    public Gson get() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Key.class, new KeyAdapter())
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .setLenient()
            .create();
    }

    /**
     * TypeAdapter for Adventure Key serialization.
     * <p>
     * Writes keys as "namespace:value" strings and reads them using {@link Key#key(String)}.
     */
    private static final class KeyAdapter extends TypeAdapter<Key> {

        @Override
        public void write(JsonWriter out, Key value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.namespace() + ":" + value.value());
        }

        @Override
        public Key read(JsonReader in) throws IOException {
            String keyString = in.nextString();
            return Key.key(keyString);
        }
    }

    /**
     * TypeAdapter for Instant serialization.
     * <p>
     * Writes instants as ISO-8601 strings and reads them using {@link Instant#parse(CharSequence)}.
     */
    private static final class InstantAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toString());
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            String instantString = in.nextString();
            return Instant.parse(instantString);
        }
    }
}
