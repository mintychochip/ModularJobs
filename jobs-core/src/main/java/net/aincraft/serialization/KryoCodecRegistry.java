package net.aincraft.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.aincraft.boost.AdditiveBoostImpl;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.conditions.BiomeConditionImpl;
import net.aincraft.boost.conditions.ComposableConditionImpl;
import net.aincraft.boost.conditions.LiquidConditionImpl;
import net.aincraft.boost.conditions.NegatingConditionImpl;
import net.aincraft.boost.conditions.PlayerResourceConditionImpl;
import net.aincraft.boost.conditions.PotionConditionImpl;
import net.aincraft.boost.conditions.PotionTypeConditionImpl;
import net.aincraft.boost.conditions.SneakConditionImpl;
import net.aincraft.boost.conditions.SprintConditionImpl;
import net.aincraft.boost.conditions.WeatherConditionImpl;
import net.aincraft.boost.conditions.WorldConditionImpl;
import net.aincraft.boost.policy.AllApplicablePolicyImpl;
import net.aincraft.boost.policy.GetFirstPolicyImpl;
import net.aincraft.boost.policy.TopKPolicyImpl;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostSource;
import net.aincraft.container.SlotSet;
import net.aincraft.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.PassiveBoostData;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.RuledBoostSource.Policy;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.WeatherState;
import net.aincraft.container.Store;
import net.aincraft.job.SlotSetImpl;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public final class KryoCodecRegistry implements CodecRegistry {

  private static final int BUFFER_SIZE = 256;

  private final ThreadLocal<Kryo> kryoThreadLocal;
  private final Cache<ByteBuffer, Object> decodeCache;

  public KryoCodecRegistry() {
    this.kryoThreadLocal = ThreadLocal.withInitial(this::createKryo);
    this.decodeCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterAccess(java.time.Duration.ofMinutes(1))
        .build();
  }

  private Kryo createKryo() {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(true);
    kryo.setReferences(false);

    int id = 10;

    // Primitives & common types
    kryo.register(BigDecimal.class, new BigDecimalSerializer(), id++);
    kryo.register(BigInteger.class, id++);
    kryo.register(Duration.class, new DurationSerializer(), id++);
    kryo.register(NamespacedKey.class, new KeySerializer(), id++);
    try {
      kryo.register(Class.forName("net.kyori.adventure.key.KeyImpl"), new KeySerializer(), id++);
    } catch (ClassNotFoundException ignored) {
      // KeyImpl not available in this environment
    }
    kryo.register(ArrayList.class, id++);
    kryo.register(HashMap.class, id++);
    kryo.register(Material.class, new MaterialSerializer(), id++);
    kryo.register(PotionEffectType.class, new PotionEffectTypeSerializer(), id++);

    // Enums
    kryo.register(LogicalOperator.class, id++);
    kryo.register(PlayerResourceType.class, id++);
    kryo.register(RelationalOperator.class, id++);
    kryo.register(WeatherState.class, id++);
    kryo.register(PotionConditionType.class, id++);

    // SlotSet
    kryo.register(SlotSetImpl.class, new SlotSetSerializer(), id++);

    // Boosts
    kryo.register(AdditiveBoostImpl.class, new AdditiveBoostSerializer(), id++);
    kryo.register(MultiplicativeBoostImpl.class, new MultiplicativeBoostSerializer(), id++);

    // Boost data
    kryo.register(ConsumableBoostData.class, new ConsumableBoostDataSerializer(), id++);
    kryo.register(PassiveBoostData.class, new PassiveBoostDataSerializer(), id++);

    // Policies
    kryo.register(AllApplicablePolicyImpl.class, id++);
    kryo.register(GetFirstPolicyImpl.class, id++);
    kryo.register(TopKPolicyImpl.class, new TopKPolicySerializer(), id++);

    // Conditions
    kryo.register(ComposableConditionImpl.class, new ComposableConditionSerializer(), id++);
    kryo.register(NegatingConditionImpl.class, new NegatingConditionSerializer(), id++);
    kryo.register(SneakConditionImpl.class, new SneakConditionSerializer(), id++);
    kryo.register(SprintConditionImpl.class, new SprintConditionSerializer(), id++);
    kryo.register(WorldConditionImpl.class, new WorldConditionSerializer(), id++);
    kryo.register(BiomeConditionImpl.class, new BiomeConditionSerializer(), id++);
    kryo.register(PlayerResourceConditionImpl.class, new PlayerResourceConditionSerializer(), id++);
    kryo.register(PotionTypeConditionImpl.class, new PotionTypeConditionSerializer(), id++);
    kryo.register(PotionConditionImpl.class, new PotionConditionSerializer(), id++);
    kryo.register(LiquidConditionImpl.class, new LiquidConditionSerializer(), id++);
    kryo.register(WeatherConditionImpl.class, new WeatherConditionSerializer(), id++);

    // Rules & RuledBoostSource
    kryo.register(Rule.class, new RuleSerializer(), id++);
    kryo.register(RuledBoostSourceImpl.class, new RuledBoostSourceSerializer(), id++);

    return kryo;
  }

  @Override
  public byte @NotNull [] encode(Object object) {
    Kryo kryo = kryoThreadLocal.get();
    try (Output output = new Output(BUFFER_SIZE, -1)) {
      kryo.writeClassAndObject(output, object);
      return output.toBytes();
    }
  }

  @Override
  public Object decode(byte[] bytes) {
    return decodeCache.get(ByteBuffer.wrap(bytes), ignored -> {
      Kryo kryo = kryoThreadLocal.get();
      try (Input input = new Input(bytes)) {
        return kryo.readClassAndObject(input);
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T decode(byte[] bytes, Class<T> clazz) {
    return (T) decode(bytes);
  }

  // ============ Custom Serializers ============

  private static final class BigDecimalSerializer extends Serializer<BigDecimal> {
    @Override
    public void write(Kryo kryo, Output output, BigDecimal value) {
      if (value.signum() == 0) {
        output.writeVarInt(0, true);
        return;
      }
      BigDecimal n = value.stripTrailingZeros();
      byte[] unscaled = n.unscaledValue().toByteArray();
      output.writeVarInt(n.scale(), false);
      output.writeVarInt(unscaled.length, true);
      output.writeBytes(unscaled);
    }

    @Override
    public BigDecimal read(Kryo kryo, Input input, Class<? extends BigDecimal> type) {
      int scale = input.readVarInt(false);
      int len = input.readVarInt(true);
      if (len == 0) {
        return BigDecimal.ZERO;
      }
      byte[] unscaled = input.readBytes(len);
      return new BigDecimal(new BigInteger(unscaled), scale);
    }
  }

  private static final class DurationSerializer extends Serializer<Duration> {
    @Override
    public void write(Kryo kryo, Output output, Duration value) {
      output.writeVarLong(value.getSeconds(), false);
      output.writeVarInt(value.getNano(), true);
    }

    @Override
    public Duration read(Kryo kryo, Input input, Class<? extends Duration> type) {
      long seconds = input.readVarLong(false);
      int nanos = input.readVarInt(true);
      return Duration.ofSeconds(seconds, nanos);
    }
  }

  private static final class KeySerializer extends Serializer<Key> {
    @Override
    public void write(Kryo kryo, Output output, Key key) {
      String ns = key.namespace();
      boolean minecraft = "minecraft".equals(ns);
      output.writeBoolean(minecraft);
      if (!minecraft) {
        output.writeString(ns);
      }
      output.writeString(key.value());
    }

    @Override
    public Key read(Kryo kryo, Input input, Class<? extends Key> type) {
      boolean minecraft = input.readBoolean();
      String ns = minecraft ? "minecraft" : input.readString();
      String value = input.readString();
      return new NamespacedKey(ns, value);
    }
  }

  private static final class MaterialSerializer extends Serializer<Material> {
    @Override
    public void write(Kryo kryo, Output output, Material material) {
      output.writeString(material.key().value());
    }

    @Override
    public Material read(Kryo kryo, Input input, Class<? extends Material> type) {
      String value = input.readString();
      return Registry.MATERIAL.get(NamespacedKey.minecraft(value));
    }
  }

  private static final class PotionEffectTypeSerializer extends Serializer<PotionEffectType> {
    @Override
    public void write(Kryo kryo, Output output, PotionEffectType pet) {
      output.writeString(pet.key().value());
    }

    @Override
    public PotionEffectType read(Kryo kryo, Input input, Class<? extends PotionEffectType> type) {
      String value = input.readString();
      return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(value));
    }
  }

  private static final class SlotSetSerializer extends Serializer<SlotSetImpl> {
    @Override
    public void write(Kryo kryo, Output output, SlotSetImpl value) {
      output.writeVarLong(value.asLong(), false);
    }

    @Override
    public SlotSetImpl read(Kryo kryo, Input input, Class<? extends SlotSetImpl> type) {
      return new SlotSetImpl(input.readVarLong(false));
    }
  }

  private static final class AdditiveBoostSerializer extends Serializer<AdditiveBoostImpl> {
    @Override
    public void write(Kryo kryo, Output output, AdditiveBoostImpl value) {
      kryo.writeObject(output, value.amount());
    }

    @Override
    public AdditiveBoostImpl read(Kryo kryo, Input input, Class<? extends AdditiveBoostImpl> type) {
      return new AdditiveBoostImpl(kryo.readObject(input, BigDecimal.class));
    }
  }

  private static final class MultiplicativeBoostSerializer extends Serializer<MultiplicativeBoostImpl> {
    @Override
    public void write(Kryo kryo, Output output, MultiplicativeBoostImpl value) {
      kryo.writeObject(output, value.amount());
    }

    @Override
    public MultiplicativeBoostImpl read(Kryo kryo, Input input, Class<? extends MultiplicativeBoostImpl> type) {
      return new MultiplicativeBoostImpl(kryo.readObject(input, BigDecimal.class));
    }
  }

  private static final class ConsumableBoostDataSerializer extends Serializer<ConsumableBoostData> {
    @Override
    public void write(Kryo kryo, Output output, ConsumableBoostData value) {
      kryo.writeClassAndObject(output, value.boostSource());
      kryo.writeObject(output, value.duration());
    }

    @Override
    public ConsumableBoostData read(Kryo kryo, Input input, Class<? extends ConsumableBoostData> type) {
      BoostSource boostSource = (BoostSource) kryo.readClassAndObject(input);
      Duration duration = kryo.readObject(input, Duration.class);
      return new ConsumableBoostData(boostSource, duration);
    }
  }

  private static final class PassiveBoostDataSerializer extends Serializer<PassiveBoostData> {
    @Override
    public void write(Kryo kryo, Output output, PassiveBoostData value) {
      kryo.writeClassAndObject(output, value.boostSource());
      kryo.writeObject(output, (SlotSetImpl) value.slotSet());
    }

    @Override
    public PassiveBoostData read(Kryo kryo, Input input, Class<? extends PassiveBoostData> type) {
      BoostSource boostSource = (BoostSource) kryo.readClassAndObject(input);
      SlotSet slotSet = kryo.readObject(input, SlotSetImpl.class);
      return new PassiveBoostData(boostSource, slotSet);
    }
  }

  private static final class TopKPolicySerializer extends Serializer<TopKPolicyImpl> {
    @Override
    public void write(Kryo kryo, Output output, TopKPolicyImpl value) {
      output.writeVarInt(value.k(), true);
    }

    @Override
    public TopKPolicyImpl read(Kryo kryo, Input input, Class<? extends TopKPolicyImpl> type) {
      return new TopKPolicyImpl(input.readVarInt(true));
    }
  }

  private static final class ComposableConditionSerializer extends Serializer<ComposableConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, ComposableConditionImpl value) {
      kryo.writeObject(output, value.logicalOperator());
      kryo.writeClassAndObject(output, value.a());
      kryo.writeClassAndObject(output, value.b());
    }

    @Override
    public ComposableConditionImpl read(Kryo kryo, Input input, Class<? extends ComposableConditionImpl> type) {
      LogicalOperator operator = kryo.readObject(input, LogicalOperator.class);
      Condition a = (Condition) kryo.readClassAndObject(input);
      Condition b = (Condition) kryo.readClassAndObject(input);
      return new ComposableConditionImpl(a, b, operator);
    }
  }

  private static final class NegatingConditionSerializer extends Serializer<NegatingConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, NegatingConditionImpl value) {
      kryo.writeClassAndObject(output, value.condition());
    }

    @Override
    public NegatingConditionImpl read(Kryo kryo, Input input, Class<? extends NegatingConditionImpl> type) {
      Condition condition = (Condition) kryo.readClassAndObject(input);
      return new NegatingConditionImpl(condition);
    }
  }

  private static final class SneakConditionSerializer extends Serializer<SneakConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, SneakConditionImpl value) {
      output.writeBoolean(value.state());
    }

    @Override
    public SneakConditionImpl read(Kryo kryo, Input input, Class<? extends SneakConditionImpl> type) {
      return new SneakConditionImpl(input.readBoolean());
    }
  }

  private static final class SprintConditionSerializer extends Serializer<SprintConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, SprintConditionImpl value) {
      output.writeBoolean(value.state());
    }

    @Override
    public SprintConditionImpl read(Kryo kryo, Input input, Class<? extends SprintConditionImpl> type) {
      return new SprintConditionImpl(input.readBoolean());
    }
  }

  private static final class WorldConditionSerializer extends Serializer<WorldConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, WorldConditionImpl value) {
      kryo.writeObject(output, value.worldKey());
    }

    @Override
    public WorldConditionImpl read(Kryo kryo, Input input, Class<? extends WorldConditionImpl> type) {
      Key key = kryo.readObject(input, NamespacedKey.class);
      return new WorldConditionImpl(key);
    }
  }

  private static final class BiomeConditionSerializer extends Serializer<BiomeConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, BiomeConditionImpl value) {
      kryo.writeObject(output, value.biomeKey());
    }

    @Override
    public BiomeConditionImpl read(Kryo kryo, Input input, Class<? extends BiomeConditionImpl> type) {
      Key biomeKey = kryo.readObject(input, NamespacedKey.class);
      return new BiomeConditionImpl(biomeKey);
    }
  }

  private static final class PlayerResourceConditionSerializer extends Serializer<PlayerResourceConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, PlayerResourceConditionImpl value) {
      kryo.writeObject(output, value.type());
      output.writeDouble(value.expected());
      kryo.writeObject(output, value.operator());
    }

    @Override
    public PlayerResourceConditionImpl read(Kryo kryo, Input input, Class<? extends PlayerResourceConditionImpl> type) {
      PlayerResourceType resourceType = kryo.readObject(input, PlayerResourceType.class);
      double expected = input.readDouble();
      RelationalOperator operator = kryo.readObject(input, RelationalOperator.class);
      return new PlayerResourceConditionImpl(resourceType, expected, operator);
    }
  }

  private static final class PotionTypeConditionSerializer extends Serializer<PotionTypeConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, PotionTypeConditionImpl value) {
      output.writeString(value.type().key().value());
    }

    @Override
    public PotionTypeConditionImpl read(Kryo kryo, Input input, Class<? extends PotionTypeConditionImpl> type) {
      String value = input.readString();
      PotionEffectType pet = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(value));
      return new PotionTypeConditionImpl(pet);
    }
  }

  private static final class PotionConditionSerializer extends Serializer<PotionConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, PotionConditionImpl value) {
      output.writeString(value.type().key().value());
      output.writeVarInt(value.expected(), true);
      kryo.writeObject(output, value.conditionType());
      kryo.writeObject(output, value.relationalOperator());
    }

    @Override
    public PotionConditionImpl read(Kryo kryo, Input input, Class<? extends PotionConditionImpl> type) {
      String value = input.readString();
      PotionEffectType pet = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(value));
      int expected = input.readVarInt(true);
      PotionConditionType conditionType = kryo.readObject(input, PotionConditionType.class);
      RelationalOperator operator = kryo.readObject(input, RelationalOperator.class);
      return new PotionConditionImpl(pet, expected, conditionType, operator);
    }
  }

  private static final class LiquidConditionSerializer extends Serializer<LiquidConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, LiquidConditionImpl value) {
      kryo.writeObject(output, value.liquid());
    }

    @Override
    public LiquidConditionImpl read(Kryo kryo, Input input, Class<? extends LiquidConditionImpl> type) {
      Material liquid = kryo.readObject(input, Material.class);
      return new LiquidConditionImpl(liquid);
    }
  }

  private static final class WeatherConditionSerializer extends Serializer<WeatherConditionImpl> {
    @Override
    public void write(Kryo kryo, Output output, WeatherConditionImpl value) {
      kryo.writeObject(output, value.state());
    }

    @Override
    public WeatherConditionImpl read(Kryo kryo, Input input, Class<? extends WeatherConditionImpl> type) {
      WeatherState state = kryo.readObject(input, WeatherState.class);
      return new WeatherConditionImpl(state);
    }
  }

  private static final class RuleSerializer extends Serializer<Rule> {
    @Override
    public void write(Kryo kryo, Output output, Rule value) {
      output.writeVarInt(value.priority(), true);
      kryo.writeClassAndObject(output, value.condition());
      kryo.writeClassAndObject(output, value.boost());
    }

    @Override
    public Rule read(Kryo kryo, Input input, Class<? extends Rule> type) {
      int priority = input.readVarInt(true);
      Condition condition = (Condition) kryo.readClassAndObject(input);
      Boost boost = (Boost) kryo.readClassAndObject(input);
      return new Rule(condition, priority, boost);
    }
  }

  private static final class RuledBoostSourceSerializer extends Serializer<RuledBoostSourceImpl> {
    @Override
    public void write(Kryo kryo, Output output, RuledBoostSourceImpl value) {
      kryo.writeClassAndObject(output, value.policy());
      kryo.writeObject(output, value.key());
      output.writeString(value.description());
      List<Rule> rules = value.rules();
      output.writeVarInt(rules.size(), true);
      for (Rule rule : rules) {
        kryo.writeObject(output, rule);
      }
    }

    @Override
    public RuledBoostSourceImpl read(Kryo kryo, Input input, Class<? extends RuledBoostSourceImpl> type) {
      Policy policy = (Policy) kryo.readClassAndObject(input);
      Key key = kryo.readObject(input, NamespacedKey.class);
      String description = input.readString();
      int size = input.readVarInt(true);
      List<Rule> rules = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        rules.add(kryo.readObject(input, Rule.class));
      }
      return new RuledBoostSourceImpl(rules, policy, key, description);
    }
  }

}
