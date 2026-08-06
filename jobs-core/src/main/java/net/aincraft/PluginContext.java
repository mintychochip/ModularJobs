package net.aincraft;

import com.google.gson.Gson;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.aincraft.boost.BoostFactoryImpl;
import net.aincraft.boost.ConsumableBoostController;
import net.aincraft.boost.config.BoostSourceLoader;
import net.aincraft.commands.ApplyEditsCommand;
import net.aincraft.commands.ArchiveCommand;
import net.aincraft.commands.BoostCommand;
import net.aincraft.commands.BrowseCommand;
import net.aincraft.commands.DialogNavigationListener;
import net.aincraft.commands.EditorCommand;
import net.aincraft.commands.ExperienceCommand;
import net.aincraft.commands.InfoCommand;
import net.aincraft.commands.JoinCommand;
import net.aincraft.commands.JobsCommand;
import net.aincraft.commands.JobTopPageProvider;
import net.aincraft.commands.LeaveCommand;
import net.aincraft.commands.LevelCommand;
import net.aincraft.commands.ListCommand;
import net.aincraft.commands.StatsCommand;
import net.aincraft.commands.StatsDialog;
import net.aincraft.commands.TopCommand;
import net.aincraft.commands.TreeEditorCommand;
import net.aincraft.commands.UpgradesCommand;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.container.ActionType;
import net.aincraft.container.BoostSource;
import net.aincraft.container.PayableType;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.domain.DomainWiring;
import net.aincraft.editor.BytebinClient;
import net.aincraft.editor.BytebinClient;
import net.aincraft.editor.EditorConfig;
import net.aincraft.editor.EditorService;
import net.aincraft.editor.EditorService;
import net.aincraft.editor.EditorSessionStore;
import net.aincraft.editor.json.GsonProvider;
import net.aincraft.gui.JobBrowseGui;
import net.aincraft.gui.UpgradeTreeGui;
import net.aincraft.payable.PayableWiring;
import net.aincraft.payment.PaymentWiring;
import net.aincraft.placeholders.ModularJobsPlaceholderExpansion;
import net.aincraft.profession.ProfessionWiring;
import net.aincraft.protection.BlockOwnershipService;
import net.aincraft.protection.BlockOwnershipService;
import net.aincraft.protection.BlockProtectionAdapter;
import net.aincraft.protection.BlockProtectionAdapterProvider;
import net.aincraft.registry.ActionTypeRegistryProvider;
import net.aincraft.registry.Registry;
import net.aincraft.registry.RegistryContainerImpl;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.registry.SimpleRegistryImpl;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.repository.ConnectionSourceFactory;
import net.aincraft.repository.DatabaseConfigSections;
import net.aincraft.repository.PluginResources;
import net.aincraft.repository.RelationalTimedBoostRepositoryImpl;
import net.aincraft.serialization.KryoCodecRegistry;
import net.aincraft.serialization.KryoCodecRegistry;
import net.aincraft.service.ItemBoostDataServiceImpl;
import net.aincraft.service.PreferencesService;
import net.aincraft.service.PreferencesServiceImpl;
import net.aincraft.service.TimedBoostDataServiceImpl;
import net.aincraft.upgrade.PlayerUpgradeRepository;
import net.aincraft.upgrade.PlayerUpgradeRepository;
import net.aincraft.upgrade.UpgradeBoostDataService;
import net.aincraft.upgrade.UpgradeBoostDataServiceImpl;
import net.aincraft.upgrade.UpgradeEffectApplier;
import net.aincraft.upgrade.UpgradeLevelUpListener;
import net.aincraft.upgrade.UpgradePermissionManager;
import net.aincraft.upgrade.UpgradePermissionRestoreListener;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeServiceImpl;
import net.aincraft.upgrade.UpgradeTree;
import net.aincraft.upgrade.config.UpgradeTreeLoader;
import net.aincraft.upgrade.editor.TreeEditorExporter;
import net.aincraft.upgrade.editor.TreeEditorGui;
import net.aincraft.upgrade.editor.TreeEditorNodeGui;
import net.aincraft.upgrade.editor.TreeEditorSettingsGui;
import net.aincraft.util.KeyResolver;
import net.aincraft.util.KeyResolvers;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * Manual composition root for ModularJobs (constructor wiring; no DI framework).
 */
public final class PluginContext {

  public final Bridge bridge;
  /** Primary payable ConnectionSource (also first entry in {@link #resources}). */
  public final ConnectionSource connectionSource;
  /** All DB sources + write-back flush hooks owned by this composition. */
  public final PluginResources resources;
  public final UpgradeTreeLoader upgradeTreeLoader;
  public final Set<Listener> listeners;
  public final Set<JobsCommand> commands;
  public final Command testCommand;
  @Nullable
  public final PlaceholderExpansion placeholderExpansion;

  private PluginContext(
      Bridge bridge,
      ConnectionSource connectionSource,
      PluginResources resources,
      UpgradeTreeLoader upgradeTreeLoader,
      Set<Listener> listeners,
      Set<JobsCommand> commands,
      Command testCommand,
      @Nullable PlaceholderExpansion placeholderExpansion) {
    this.bridge = bridge;
    this.connectionSource = connectionSource;
    this.resources = resources;
    this.upgradeTreeLoader = upgradeTreeLoader;
    this.listeners = listeners;
    this.commands = commands;
    this.testCommand = testCommand;
    this.placeholderExpansion = placeholderExpansion;
  }

  /**
   * Flush write-backs and shut down every tracked ConnectionSource.
   * Same path bootstrap uses on disable.
   */
  public void shutdown() throws SQLException {
    resources.shutdown();
  }

  public static PluginContext create(JavaPlugin plugin) {
    PluginResources resources = new PluginResources();
    try {
      return createInto(plugin, resources);
    } catch (Throwable t) {
      resources.closeQuietly();
      throw t;
    }
  }

  /**
   * Composition body. Sources are tracked on {@code resources} as they open so callers
   * (and {@link #create}) can clean up on failure.
   */
  static PluginContext createInto(JavaPlugin plugin, PluginResources resources) {
    YamlConfiguration databaseConfig = YamlConfiguration.create(plugin, "database.yml");
    plugin.getSLF4JLogger().info("Loading database.yml, keys: {}", databaseConfig.getKeys(false));

    ConfigurationSection payableSection =
        DatabaseConfigSections.requireSection(databaseConfig, "payable");
    ConnectionSource connectionSource = resources.track(
        new ConnectionSourceFactory(plugin, payableSection).create());

    KryoCodecRegistry codecRegistry = new KryoCodecRegistry();
    KeyResolver keyResolver = KeyResolvers.create();
    BoostFactory boostFactory = BoostFactoryImpl.INSTANCE;
    ConditionFactory conditionFactory = BoostFactoryImpl.INSTANCE;
    Gson gson = GsonProvider.create();

    Registry<ActionType> actionTypeRegistry = ActionTypeRegistryProvider.create(plugin);

    // JobService holds a reference to the payable registry; types are registered after domain
    // construction so the experience handler can close over JobService.
    Registry<PayableType> payableTypeRegistry = new SimpleRegistryImpl<>();

    DomainWiring domain = DomainWiring.create(
        plugin,
        connectionSource,
        resources,
        actionTypeRegistry,
        payableTypeRegistry,
        keyResolver);

    PayableWiring payables =
        PayableWiring.create(plugin, domain.jobService, payableTypeRegistry);

    RegistryContainerImpl registryContainer = new RegistryContainerImpl();
    registryContainer.addRegistry(RegistryKeys.ACTION_TYPES.key(), actionTypeRegistry);
    registryContainer.addRegistry(RegistryKeys.PAYABLE_TYPES.key(), payableTypeRegistry);

    ConfigurationSection timedBoostSection =
        DatabaseConfigSections.requireSection(databaseConfig, "timed-boost");
    ConnectionSource timedBoostSource = resources.track(
        new ConnectionSourceFactory(plugin, timedBoostSection).create());
    RelationalTimedBoostRepositoryImpl timedBoostRepository = new RelationalTimedBoostRepositoryImpl(
        plugin, timedBoostSource, codecRegistry);
    resources.onFlush(timedBoostRepository::flushPending);
    TimedBoostDataService timedBoostDataService =
        new TimedBoostDataServiceImpl(timedBoostRepository);
    ItemBoostDataService itemBoostDataService = new ItemBoostDataServiceImpl(codecRegistry);

    PreferencesService preferencesService = new PreferencesServiceImpl(plugin);

    ConfigurationSection upgradesSection = DatabaseConfigSections.sectionOrFallback(
        databaseConfig, "upgrades", payableSection);
    ConnectionSource upgradeConnection = resources.track(
        new ConnectionSourceFactory(plugin, upgradesSection).create());
    PlayerUpgradeRepository playerUpgradeRepository =
        new PlayerUpgradeRepository(upgradeConnection);

    Registry<UpgradeTree> upgradeTreeRegistry = new SimpleRegistryImpl<>();
    UpgradeTreeLoader upgradeTreeLoader = new UpgradeTreeLoader(
        plugin, gson, upgradeTreeRegistry, conditionFactory, boostFactory);
    upgradeTreeLoader.load();

    UpgradePermissionManager permissionManager = new UpgradePermissionManager(plugin);
    UpgradeEffectApplier effectApplier = new UpgradeEffectApplier(permissionManager);
    UpgradeBoostDataService upgradeBoostDataService =
        new UpgradeBoostDataServiceImpl(playerUpgradeRepository, upgradeTreeRegistry);
    UpgradeService upgradeService = new UpgradeServiceImpl(
        upgradeTreeRegistry, playerUpgradeRepository, domain.jobService, effectApplier);

    UpgradeTreeGui upgradeTreeGui = new UpgradeTreeGui(plugin, upgradeService);
    TreeEditorExporter treeEditorExporter = new TreeEditorExporter();
    TreeEditorNodeGui treeEditorNodeGui = new TreeEditorNodeGui(plugin);
    TreeEditorSettingsGui treeEditorSettingsGui = new TreeEditorSettingsGui(plugin);
    TreeEditorGui treeEditorGui = new TreeEditorGui(
        plugin, treeEditorExporter, upgradeTreeLoader, treeEditorNodeGui, treeEditorSettingsGui);

    Registry<BoostSource> boostSourceRegistry = new SimpleRegistryImpl<>();
    BoostSourceLoader boostSourceLoader = new BoostSourceLoader(
        plugin, gson, conditionFactory, boostFactory, boostSourceRegistry);
    boostSourceLoader.load();

    BlockProtectionAdapter protectionAdapter = BlockProtectionAdapterProvider.create();
    BlockOwnershipService blockOwnershipService =
        new BlockOwnershipService(protectionAdapter);

    ProfessionWiring professions = ProfessionWiring.create(domain.jobService);

    PaymentWiring payment = PaymentWiring.create(
        plugin,
        domain.jobService,
        itemBoostDataService,
        timedBoostDataService,
        upgradeBoostDataService,
        blockOwnershipService,
        professions.recipeService,
        professions.professionService);

    EditorConfig editorConfig = EditorConfig.defaults();
    EditorSessionStore sessionStore = new EditorSessionStore(editorConfig);
    BytebinClient bytebinClient = new BytebinClient(editorConfig, gson);
    EditorService editorService = new EditorService(
        domain.jobService,
        domain.jobTaskRepository,
        bytebinClient,
        sessionStore,
        editorConfig,
        gson);

    JobBrowseGui jobBrowseGui = new JobBrowseGui(domain.jobService, upgradeService);
    StatsDialog statsDialog = new StatsDialog();
    JobTopPageProvider topPageProvider = new JobTopPageProvider(domain.jobService);

    InfoCommand infoCommand = new InfoCommand(
        domain.jobService, domain.jobResolver, preferencesService);

    Set<JobsCommand> commands = new LinkedHashSet<>();
    commands.add(new JoinCommand(domain.jobService, domain.jobResolver));
    commands.add(new ListCommand(domain.jobService));
    commands.add(new BrowseCommand(jobBrowseGui));
    commands.add(new TopCommand(domain.jobService, topPageProvider, plugin));
    commands.add(infoCommand);
    commands.add(new LeaveCommand(domain.jobService, domain.jobResolver));
    commands.add(new ApplyEditsCommand(editorService));
    commands.add(new EditorCommand(editorService, domain.jobService, domain.jobResolver));
    commands.add(new StatsCommand(domain.jobService, statsDialog));
    commands.add(new ArchiveCommand(domain.jobService));
    commands.add(new BoostCommand(
        boostSourceRegistry,
        timedBoostDataService,
        itemBoostDataService,
        boostSourceLoader,
        upgradeBoostDataService,
        domain.jobService));
    commands.add(new UpgradesCommand(upgradeService, domain.jobResolver, upgradeTreeGui));
    commands.add(new TreeEditorCommand(
        upgradeService, domain.jobResolver, treeEditorGui, upgradeTreeLoader));
    commands.add(new LevelCommand(domain.jobService, domain.progressionService));
    commands.add(new ExperienceCommand(domain.jobService, domain.progressionService));

    List<Listener> listenerList = new ArrayList<>();
    listenerList.addAll(payment.listeners);
    listenerList.add(new ConsumableBoostController(itemBoostDataService, timedBoostDataService));
    listenerList.add(new DialogNavigationListener(
        domain.jobService, domain.jobResolver, infoCommand, statsDialog, preferencesService));
    listenerList.add(new UpgradeLevelUpListener(upgradeService));
    listenerList.add(upgradeTreeGui);
    listenerList.add(new UpgradePermissionRestoreListener(
        upgradeService, effectApplier, permissionManager));

    Bridge bridge = new BridgeImpl(
        plugin,
        registryContainer,
        domain.jobService,
        professions.professionService,
        professions.recipeService,
        professions.buffService,
        professions.stationService,
        professions.nodeHarvestService,
        payables.economyProvider,
        conditionFactory,
        boostFactory,
        timedBoostDataService);

    Command testCommand = new Command(
        domain.jobService, codecRegistry, itemBoostDataService);

    PlaceholderExpansion placeholderExpansion = null;
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      placeholderExpansion = new ModularJobsPlaceholderExpansion(domain.jobService);
    }

    return new PluginContext(
        bridge,
        connectionSource,
        resources,
        upgradeTreeLoader,
        new LinkedHashSet<>(listenerList),
        commands,
        testCommand,
        placeholderExpansion);
  }
}
