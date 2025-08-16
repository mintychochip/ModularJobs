package net.aincraft.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.Condition.Codec;
import net.aincraft.api.container.boost.Condition.Codec.Typed;
import net.aincraft.api.container.boost.Condition.Codec.Typed.Reader;
import net.aincraft.api.container.boost.Condition.Codec.Typed.Writer;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.aincraft.api.registry.Registry;
import net.aincraft.api.service.CodecRegistry;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class CodecRegistryImpl implements CodecRegistry, Writer,
    Reader {

  private static final int MAX_ADAPTERS = 256;
  private final Cache<ByteBuffer, Condition> decodeCache = Caffeine
      .newBuilder()
      .maximumSize(500)
      .expireAfterAccess(Duration.ofMinutes(5))
      .build();

  private final Registry<Codec> codecs = Registry.simple();
  private final Map<Class<?>, Key> typeToKey = new HashMap<>();
  private int nextId = 0;

  @Override
  public byte[] encode(Condition condition) {
    Out out = new Out(64);
    write(out, condition);
    return out.toByteArray();
  }

  @Override
  public Condition decode(byte[] bytes) {
    return decodeCache.get(ByteBuffer.wrap(bytes), ignored -> read(new In(bytes)));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void write(Out out, Condition condition) {
    Bukkit.broadcastMessage(codecs.toString());
    Bukkit.broadcastMessage(typeToKey.toString());
    Key key = typeToKey.get(condition.getClass());
    if (key == null) {
      throw new IllegalArgumentException(
          "could not find adapter id for type: " + condition.getClass());
    }
    Codec codec = codecs.getOrThrow(key);
    if (!(codec instanceof Typed<?>)) {
      return;
    }
    Typed<Condition> typedCodec = (Typed<Condition>) codec;
    out.writeKey(key);
    typedCodec.encode(out, condition, this);
  }

  @Override
  public Condition read(In in) {
    Key key = in.readKey();
    Codec codec = codecs.getOrThrow(key);
    if (!(codec instanceof Typed<?> typed)) {
      return null;
    }
    return typed.decode(in, this);
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
}
