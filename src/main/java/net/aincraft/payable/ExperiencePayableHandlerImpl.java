package net.aincraft.payable;

import java.math.BigDecimal;
import net.aincraft.api.container.PayableHandler;
import net.aincraft.service.ProgressionService;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class ExperiencePayableHandlerImpl implements PayableHandler {

  private final Key key;
  private final ProgressionService progressionService;

  public ExperiencePayableHandlerImpl(Key key, ProgressionService progressionService) {
    this.key = key;
    this.progressionService = progressionService;
  }

  @Override
  public void set(OfflinePlayer player, BigDecimal amount) throws IllegalArgumentException {
    boolean exists = progressionService.exists(player);
  }

  @Override
  public void add(OfflinePlayer player, BigDecimal amount) throws IllegalArgumentException {

  }

  @Override
  public BigDecimal get(OfflinePlayer player) throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull Key key() {
    return key;
  }
}
