package net.aincraft.job;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.aincraft.config.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class JobModule extends AbstractModule {

  @Provides
  @Singleton
  public JobRepository jobRepository(Plugin plugin) {
    return YamlJobRepositoryImpl.create(YamlConfiguration.create(plugin, "jobs.yml"));
  }
}
