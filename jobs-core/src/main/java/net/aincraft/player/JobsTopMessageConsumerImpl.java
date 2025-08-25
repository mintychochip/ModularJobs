package net.aincraft.player;

import com.google.inject.Inject;
import java.util.List;
import net.aincraft.JobProgression;
import org.bukkit.entity.Player;

public class JobsTopMessageConsumerImpl implements JobsTopMessageConsumer {

  private final LineFormatter<JobProgression> progressionLineFormatter;

  @Inject
  public JobsTopMessageConsumerImpl(LineFormatter<JobProgression> progressionLineFormatter) {
    this.progressionLineFormatter = progressionLineFormatter;
  }

  @Override
  public void render(Player player, List<JobProgression> records) {
    for (JobProgression progression : records) {
    }
  }
}
