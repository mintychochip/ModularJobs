package net.aincraft;

import com.google.inject.AbstractModule;
import net.aincraft.boost.BoostModule;
import net.aincraft.boost.BoostSourceModule;
import net.aincraft.commands.CommandModule;
import net.aincraft.editor.EditorModule;
import net.aincraft.config.ConfigurationModule;
import net.aincraft.domain.DomainModule;
import net.aincraft.math.MathModule;
import net.aincraft.payable.PayableModule;
import net.aincraft.hooks.preferences.PreferenceModule;
import net.aincraft.payment.PaymentModule;
import net.aincraft.placeholders.PlaceholderAPIModule;
import net.aincraft.protection.ProtectionModule;
import net.aincraft.registry.RegistryModule;
import net.aincraft.repository.RepositoryModule;
import net.aincraft.serialization.SerializationModule;
import net.aincraft.service.ServiceModule;
import net.aincraft.util.UtilModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class PluginModule extends AbstractModule {

  private final Plugin plugin;

  public PluginModule(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  protected void configure() {
    bind(Plugin.class).toInstance(plugin);
    bind(Bridge.class).to(BridgeImpl.class);
    install(new ConfigurationModule(plugin));
    install(new PayableModule());
    install(new PaymentModule());
    install(new ProtectionModule());
    install(new RegistryModule());
    install(new RepositoryModule());
    install(new ServiceModule());
    install(new SerializationModule());
    install(new UtilModule());
    install(new CommandModule());
    install(new PreferenceModule());
    install(new DomainModule());
    install(new MathModule());
    install(new BoostModule());
    install(new BoostSourceModule());
    install(new EditorModule());
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      install(new PlaceholderAPIModule());
    }
  }
}
