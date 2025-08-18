package net.aincraft.serialization;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.hash.Hashing;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import net.aincraft.registry.Registry;
import net.aincraft.serialization.Codec.Reader;
import net.aincraft.serialization.Codec.Typed;
import net.aincraft.serialization.Codec.Writer;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

final class CodecRegistryImpl implements CodecRegistry, Writer,
    Reader {

  private static final int MAX_ADAPTERS = 256;
  private final Cache<ByteBuffer, Object> decodeCache = Caffeine
      .newBuilder()
      .maximumSize(100)
      .expireAfterAccess(Duration.ofMinutes(1))
      .build();

  private final Registry<Codec> delegate;
  private final Map<Class<?>, Key> typeToKey = new HashMap<>();
  private final BiMap<Key, Integer> keyToTag = HashBiMap.create(MAX_ADAPTERS);

  public CodecRegistryImpl(Registry<Codec> delegate) {
    this.delegate = delegate;
  }
  @Override
  public byte @NotNull [] encode(Object object) {
    BinaryOutImpl out = new BinaryOutImpl(64);
    write(out, object);
    return out.toByteArray();
  }

  @Override
  public Object decode(byte[] bytes) {
    return decodeCache.get(ByteBuffer.wrap(bytes), ignored -> new BinaryInImpl(bytes));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T decode(byte[] bytes, Class<T> clazz) {
    return (T) decodeCache.get(ByteBuffer.wrap(bytes),
        ignored -> read(new BinaryInImpl(bytes), clazz));
  }

  @Override
  public void register(@NotNull Codec object) {
    if (typeToKey.size() >= MAX_ADAPTERS) {
      throw new IllegalStateException("too many adapters");
    }
    delegate.register(object);
    Key key = object.key();
    typeToKey.put(object.type(), key);
    int tag = Hashing.murmur3_32_fixed().hashString(key.asString(), StandardCharsets.UTF_8).asInt();
    if (keyToTag.containsKey(key) || keyToTag.containsValue(tag)) {
      //TODO: use the plugin logger
      Bukkit.getLogger().info("duplicated produced by key: " + key + " tag: " + tag);
      return;
    }
    keyToTag.put(key, tag);
  }

  @Override
  public @NotNull Codec getOrThrow(Key key) throws IllegalArgumentException {
    return delegate.getOrThrow(key);
  }

  @Override
  public boolean isRegistered(Key key) {
    return delegate.isRegistered(key);
  }

  @Override
  public Stream<Codec> stream() {
    return delegate.stream();
  }

  @NotNull
  @Override
  public Iterator<Codec> iterator() {
    return delegate.iterator();
  }

  @Override
  public Object read(BinaryIn in) {
    int tag = in.readInt();
    Key key = keyToTag.inverse().get(tag);
    Codec codec = delegate.getOrThrow(key);
    if (!(codec instanceof Typed<?> typed)) {
      return null;
    }
    return typed.decode(in, this);
  }

  @Override
  public void write(BinaryOut out, Object child) {
    Key key = typeToKey.get(child.getClass());
    if (key == null) {
      throw new IllegalArgumentException(
          "could not find adapter id for type: " + child.getClass());
    }
    Codec codec = delegate.getOrThrow(key);
    if (!(codec instanceof Typed<?>)) {
      return;
    }
    @SuppressWarnings("unchecked")
    Typed<Object> typedCodec = (Typed<Object>) codec;
    int tag = keyToTag.get(key);
    out.writeInt(tag);
    typedCodec.encode(out, child, this);
  }
}
