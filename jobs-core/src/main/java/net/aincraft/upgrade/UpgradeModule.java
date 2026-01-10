package net.aincraft.upgrade;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.gui.UpgradeTreeGui;
import net.aincraft.registry.Registry;
import net.aincraft.registry.SimpleRegistryImpl;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.repository.ConnectionSourceFactory;
import net.aincraft.upgrade.config.UpgradeTreeLoader;
import net.aincraft.upgrade.editor.TreeEditorExporter;
import net.aincraft.upgrade.editor.TreeEditorGui;
import net.aincraft.upgrade.editor.TreeEditorNodeGui;
import net.aincraft.upgrade.editor.TreeEditorSettingsGui;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Guice module for upgrade system bindings.
 */
public final class UpgradeModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<Registry<UpgradeTree>>() {})
        .to(new TypeLiteral<SimpleRegistryImpl<UpgradeTree>>() {})
        .in(Singleton.class);
    bind(UpgradeService.class).to(UpgradeServiceImpl.class).in(Singleton.class);
    bind(UpgradeTreeGui.class).in(Singleton.class);

    // Tree editor
    bind(TreeEditorExporter.class).in(Singleton.class);
    bind(TreeEditorNodeGui.class).in(Singleton.class);
    bind(TreeEditorSettingsGui.class).in(Singleton.class);
    bind(TreeEditorGui.class).in(Singleton.class);

    // Effect application services
    bind(UpgradePermissionManager.class).in(Singleton.class);
    bind(UpgradeEffectApplier.class).in(Singleton.class);
    bind(UpgradeBoostDataService.class).to(UpgradeBoostDataServiceImpl.class).in(Singleton.class);

    // Register upgrade listeners
    Multibinder<Listener> listenerBinder = Multibinder.newSetBinder(binder(), Listener.class);
    listenerBinder.addBinding().to(UpgradeLevelUpListener.class);
    listenerBinder.addBinding().to(UpgradeTreeGui.class);
    listenerBinder.addBinding().to(UpgradePermissionRestoreListener.class);

    // Note: Tree editor GUIs use Triumph GUI which handles events internally,
    // so they don't need to be registered as listeners
  }

  @Provides
  @Singleton
  public PlayerUpgradeRepository playerUpgradeRepository(
      Plugin plugin,
      @Named("database") YamlConfiguration configuration) {
    ConfigurationSection section = configuration.getConfigurationSection("upgrades");
    if (section == null) {
      // Fall back to payable section if upgrades section doesn't exist
      section = configuration.getConfigurationSection("payable");
    }
    ConnectionSource connectionSource = new ConnectionSourceFactory(plugin, section).create();
    return new PlayerUpgradeRepositoryImpl(connectionSource);
  }

  @Provides
  @Singleton
  public UpgradeTreeLoader upgradeTreeLoader(
      Plugin plugin,
      Gson gson,
      Registry<UpgradeTree> registry,
      ConditionFactory conditionFactory,
      BoostFactory boostFactory) {
    UpgradeTreeLoader loader = new UpgradeTreeLoader(
        plugin, gson, registry, conditionFactory, boostFactory);
    loader.load();
    return loader;
  }
}
