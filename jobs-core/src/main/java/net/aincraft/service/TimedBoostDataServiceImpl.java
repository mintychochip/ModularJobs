package net.aincraft.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.Timed;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.aincraft.database.Repository;
import org.jetbrains.annotations.Nullable;

public class TimedBoostDataServiceImpl implements TimedBoostDataService {


  @Override
  public List<TimedActiveBoost> findGlobalBoosts() {
    return List.of();
  }

  @Override
  public List<TimedActiveBoost> findBoost(Target target) {
    return List.of();
  }

  @Override
  public <T extends Timed & SerializableBoostData> void addData(T data, Target target) {
    String identifier = target instanceof PlayerTarget ? "player" : "global";
    identifier += target.identifier();
    Optional<Duration> duration = data.getDuration();
  }

  private record ActiveBoosts(Map<String,TimedActiveBoost> boosts) {}

  static final class TimedBoostStore {

    private final Repository<String, byte[]> repository;
    private final CodecRegistry codecRegistry;

    TimedBoostStore(Repository<String, byte[]> repository, CodecRegistry codecRegistry) {
      this.repository = repository;
      this.codecRegistry = codecRegistry;
    }

    @Nullable
    private TimedBoostDataServiceImpl.ActiveBoosts loadBoosts(String identifier) {
      byte[] bytes = repository.load(identifier);
      if (bytes == null) {
        return null;
      }
      return codecRegistry.decode(bytes, ActiveBoosts.class);
    }

    public void delete(String identifier, String sourceIdentifier) {

    }
  }

//  final class BoostStoreKv implements BoostStore {
//    private final Repository<String, byte[]> repo;
//    private final Gson gson = new Gson();
//
//    BoostStoreKv(Repository<String, byte[]> repo) { this.repo = repo; }
//
//    private byte[] enc(BoostBag bag) { return gson.toJson(bag).getBytes(StandardCharsets.UTF_8); }
//    private BoostBag dec(byte[] bytes) {
//      return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), BoostBag.class);
//    }
//
//    private BoostBag loadBag(String identifier) {
//      byte[] bytes = repo.load(identifier);
//      return (bytes == null) ? new BoostBag(new HashMap<>()) : dec(bytes);
//    }
//
//    @Override
//    public void upsert(BoostRow r) {
//      BoostBag bag = loadBag(r.identifier());
//      bag.bySource().put(r.sourceId(),
//          new ActiveBoost(r.sourceId(), r.startEpochMs(), r.durationMs(), r.slotMask(), r.schemaVersion()));
//      repo.save(r.identifier(), enc(bag));     // slow write OK
//    }
//
//    @Override
//    public void delete(String identifier, String sourceId) {
//      BoostBag bag = loadBag(identifier);
//      if (bag.bySource().remove(sourceId) != null) {
//        repo.save(identifier, enc(bag));       // rewrite the bag
//      }
//    }
//
//    @Override
//    public List<BoostRow> listByIdentifier(String identifier) {
//      BoostBag bag = loadBag(identifier);
//      var out = new ArrayList<BoostRow>(bag.bySource().size());
//      for (ActiveBoost b : bag.bySource().values()) {
//        out.add(new BoostRow(identifier, b.sourceId(), b.startMs(), b.durationMs(), b.slotMask(), /*payload*/null, b.schema()));
//      }
//      return out;
//    }
//
//    @Override
//    public Optional<BoostRow> get(String identifier, String sourceId) {
//      BoostBag bag = loadBag(identifier);
//      ActiveBoost b = bag.bySource().get(sourceId);
//      return (b == null) ? Optional.empty()
//          : Optional.of(new BoostRow(identifier, b.sourceId(), b.startMs(), b.durationMs(), b.slotMask(), null, b.schema()));
//    }
//
//    @Override
//    public int deleteExpired(long nowMs) {
//      // If you need global pruning, expose prune(Target) instead,
//      // or maintain a simple catalog of identifiers.
//      return 0;
//    }
//  }
}
