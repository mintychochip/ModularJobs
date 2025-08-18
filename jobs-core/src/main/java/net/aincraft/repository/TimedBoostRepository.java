package net.aincraft.repository;

import java.util.List;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import org.jetbrains.annotations.NotNull;

public interface TimedBoostRepository {

  @NotNull
  List<ActiveBoostData> findAllBoosts(String targetIdentifier);

  ActiveBoostData findBoost(String targetIdentifier, String sourceIdentifier);

  void delete(String targetIdentifier, String sourceIdentifier);

  void addBoost(ActiveBoostData boost);
}
