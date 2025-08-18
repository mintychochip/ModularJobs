package net.aincraft.service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.util.KeyResolver;
import org.bukkit.plugin.Plugin;

public final class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(TimedBoostDataService.class).to(TimedBoostDataServiceImpl.class).in(Singleton.class);
    bind(ProgressionService.class).to(ProgressionServiceImpl.class).in(Singleton.class);
    bind(JobService.class).to(JobServiceImpl.class).in(Singleton.class);
    bind(ItemBoostDataService.class).to(ItemBoostDataServiceImpl.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  public JobTaskProvider jobTaskProvider(Plugin plugin, KeyResolver keyResolver) {
    try {
      return CSVJobTaskProviderImpl.create(plugin, keyResolver);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
