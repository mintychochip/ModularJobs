package net.aincraft.player;

import java.util.List;
import java.util.stream.Stream;
import net.aincraft.JobProgression;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface JobsTopMessageConsumer {
  void render(Player player, List<JobProgression> records);
}
