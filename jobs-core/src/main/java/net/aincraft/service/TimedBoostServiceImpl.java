package net.aincraft.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import net.aincraft.container.MemoryStoreImpl;
import net.aincraft.container.Store;
import net.aincraft.container.boost.TimedBoostData;
import net.aincraft.container.boost.TimedBoostService;
import net.aincraft.container.boost.TimedBoostService.Target.PlayerTarget;

public class TimedBoostServiceImpl implements TimedBoostService {

  private final Store<UUID, byte[]> store = new MemoryStoreImpl<>();
  private final Store<UUID, UUID> playerStore = new MemoryStoreImpl<>();
  private final CodecRegistry codecRegistry;

  public TimedBoostServiceImpl(CodecRegistry codecRegistry) {
    this.codecRegistry = codecRegistry;
  }

  @Override
  public void addBoost(TimedBoostData data, Target audience) {
    if (audience instanceof PlayerTarget playerTarget) {
      UUID uniqueId = playerTarget.player().getUniqueId();
      byte[] encoded = codecRegistry.encode(data);

    }
  }

  private record TimedBoostRow(Duration started, Duration stopped, byte[] blob) {}
}
