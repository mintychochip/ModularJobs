package net.aincraft.job;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.container.PayableCurve;
import net.aincraft.container.PayableType;
import net.aincraft.container.PayableTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.Nullable;

final class YamlJobRepositoryImpl implements JobRepository {

  private final Map<String, Job> jobs;

  public YamlJobRepositoryImpl(Map<String, Job> jobs) {
    this.jobs = jobs;
  }

  public static JobRepository create(YamlConfiguration configuration) {
    Map<String, Job> jobs = new HashMap<>();
    MiniMessage miniMessage = MiniMessage.miniMessage();
    for (String jobKey : configuration.getKeys(false)) {
      if (!configuration.contains(jobKey)) {
        continue;
      }
      Component displayName = configuration.getComponent("display-name", miniMessage);
      @Nullable Component description = configuration.getComponent("description", miniMessage);
      if (displayName == null) {
        continue;
      }
      Map<PayableType, PayableCurve> curves = new HashMap<>();
      ExpressionBuilder builder = new ExpressionBuilder(
          "level * 10 + 4 * level * level + 10 * level * level * level * level").variable("level");
      curves.put(PayableTypes.EXPERIENCE, new ExpressionPayableCurveImpl(builder.build()));
      jobs.put(jobKey, new JobImpl(jobKey, displayName, description, curves));
    }
    return new YamlJobRepositoryImpl(jobs);
  }

  @Override
  public Optional<Job> getJob(String jobKey) {
    return Optional.ofNullable(jobs.get(jobKey));
  }
}
