package net.aincraft.hooks.preferences;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.aincraft.Preference.Key;
import net.aincraft.PreferenceService;
import net.aincraft.payable.ExperienceBarColorProvider;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class PreferenceModule extends AbstractModule {

  @Provides
  @Singleton
  public ExperienceBarColorProvider experienceBarColorProvider(Plugin plugin) {
    if (Bukkit.getPluginManager().isPluginEnabled("Preferences")) {
      RegisteredServiceProvider<PreferenceService> registration = Bukkit.getServicesManager()
          .getRegistration(PreferenceService.class);
      if (registration != null) {
        PreferenceService service = registration.getProvider();
        Key<Color> key = service.register(plugin, new ExperienceBarColorPreferenceImpl());
        return new PreferenceExperienceBarColorProviderImpl(service,key);
      }
    }
    return player -> Color.BLUE;
  }
}
