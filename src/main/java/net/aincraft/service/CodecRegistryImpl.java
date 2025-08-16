package net.aincraft.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import net.aincraft.api.container.Codec;
import net.aincraft.api.container.Codec.Reader;
import net.aincraft.api.container.Codec.Typed;
import net.aincraft.api.container.Codec.Writer;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.aincraft.api.registry.Registry;
import net.aincraft.api.service.CodecRegistry;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public class CodecRegistryImpl implements CodecRegistry, Writer,
    Reader {

  private static final int MAX_ADAPTERS = 256;
  private final Cache<ByteBuffer, Object> decodeCache = Caffeine
      .newBuilder()
      .maximumSize(100)
      .expireAfterAccess(Duration.ofMinutes(1))
      .build();

  private final Registry<Codec> codecs = Registry.simple();
  private final Map<Class<?>, Key> typeToKey = new HashMap<>();
  private int nextId = 0;


  @Override
  public byte[] encode(Object object) {
    Out out = new Out(64);
    write(out, object);
    return out.toByteArray();
  }

  @Override
  public Object decode(byte[] bytes) {
    return decodeCache.get(ByteBuffer.wrap(bytes),ignored -> new In(bytes));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T decode(byte[] bytes, Class<T> clazz) {
    return (T) decodeCache.get(ByteBuffer.wrap(bytes),
        ignored -> read(new In(bytes),clazz));
  }

  @Override
  public void register(@NotNull Codec object) {
    if (nextId >= MAX_ADAPTERS) {
      throw new IllegalStateException("too many adapters");
    }
    nextId++;
    codecs.register(object);
    typeToKey.put(object.type(), object.key());
  }

  @Override
  public @NotNull Codec getOrThrow(Key key) throws IllegalArgumentException {
    return codecs.getOrThrow(key);
  }

  @Override
  public boolean isRegistered(Key key) {
    return codecs.isRegistered(key);
  }

  @Override
  public Stream<Codec> stream() {
    return codecs.stream();
  }

  @NotNull
  @Override
  public Iterator<Codec> iterator() {
    return codecs.iterator();
  }

  @Override
  public Object read(In in) {
    Key key = in.readKey();
    Codec codec = codecs.getOrThrow(key);
    if (!(codec instanceof Typed<?> typed)) {
      return null;
    }
    return typed.decode(in, this);
  }

  @Override
  public void write(Out out, Object child) {
    Key key = typeToKey.get(child.getClass());
    if (key == null) {
      throw new IllegalArgumentException(
          "could not find adapter id for type: " + child.getClass());
    }
    Codec codec = codecs.getOrThrow(key);
    if (!(codec instanceof Typed<?>)) {
      return;
    }
    @SuppressWarnings("unchecked")
    Typed<Object> typedCodec = (Typed<Object>) codec;
    out.writeKey(key);
    typedCodec.encode(out, child, this);
  }
}
