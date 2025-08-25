//package net.aincraft.internal;
//
//import java.io.IOException;
//import java.util.Optional;
//import net.aincraft.Bridge;
//import net.aincraft.Jobs;
//import net.aincraft.boost.BoostFactoryImpl;
//import net.aincraft.container.EconomyProvider;
//import net.aincraft.util.KeyResolver;
//import net.aincraft.container.PayableType;
//import net.aincraft.container.boost.TimedBoostDataService;
//import net.aincraft.container.boost.factories.BoostFactory;
//import net.aincraft.container.boost.factories.ConditionFactory;
//import net.aincraft.container.boost.factories.PolicyFactory;
//import net.aincraft.repository.ConnectionSource;
//import net.aincraft.payment.BufferedExperienceHandlerImpl;
//import net.aincraft.payment.PersistentChunkExplorationStoreImpl;
//import net.aincraft.registry.RegistryContainer;
//import net.aincraft.registry.RegistryContainerImpl;
//import net.aincraft.registry.RegistryKeys;
//import net.aincraft.registry.RegistryView;
//import net.aincraft.serialization.Codec;
//import net.aincraft.service.CSVJobTaskProviderImpl;
//import net.aincraft.serialization.CodecRegistry;
//import net.aincraft.service.JobTaskProvider;
//import net.aincraft.payment.MobDamageTracker;
//import net.aincraft.service.ProgressionService;
//import net.aincraft.service.boost.RelationalTimedBoostRepositoryImpl;
//import net.aincraft.service.TimedBoostDataServiceImpl;
//import net.aincraft.repository.RelationalProgressionRepositoryImpl;
//import net.kyori.adventure.jobKey.Key;
//
//public final class BridgeImpl implements Bridge {
//
//  private final Jobs plugin;
//  private final BridgeDependencyResolver dependencyResolver;
//  private final ConnectionSource connectionSource;
//  private final RegistryContainer registryContainer;
//  private final ProgressionService progressionService;
//  private final KeyResolver keyResolver = new KeyResolverImpl();
//  private final EconomyProvider economyProvider;
//  private final TimedBoostDataService timedBoostDataService;
//  private final JobTaskProvider jobTaskProvider;
//
//  public BridgeImpl(Jobs plugin, ConnectionSource connectionSource) {
//    this.plugin = plugin;
//    dependencyResolver = new BridgeDependencyResolverImpl(plugin);
//    economyProvider = dependencyResolver.getEconomyProvider().orElse(null);
//    this.connectionSource = connectionSource;
//
//    try {
//      jobTaskProvider = CSVJobTaskProviderImpl.create(plugin);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//
//    registryContainer = RegistryContainerImpl.create();
//    progressionService = new ProgressionServiceImpl(new RelationalProgressionRepositoryImpl(plugin,
//        connectionSource, registryContainer.getRegistry(RegistryKeys.JOBS)));
//    RegistryView<Codec> codec = registryContainer.getRegistry(RegistryKeys.CODEC);
//    CodecRegistry codecRegistry = (CodecRegistry) codec;
//    timedBoostDataService = new TimedBoostDataServiceImpl(new RelationalTimedBoostRepositoryImpl(plugin,connectionSource,codecRegistry));
//    initializeRegistryContainer();
//    new RepositoryFactoryImpl(plugin,);
//  }
//
//  private void initializeRegistryContainer() {

//    dependencyResolver.getMcMMOBoostSource().ifPresent(source -> {
//      registryContainer.editRegistry(RegistryKeys.TRANSIENT_BOOST_SOURCES, r -> {
//        r.register(source);
//      });
//    });
//  }
//
//  @Override
//  public Jobs plugin() {
//    return plugin;
//  }
//
//  @Override
//  public KeyResolver resolver() {
//    return keyResolver;
//  }
//
//  @Override
//  public ProgressionService progressionService() {
//    return progressionService;
//  }
//
//  @Override
//  public RegistryContainer registryContainer() {
//    return registryContainer;
//  }
//  @Override
//  public Optional<net.aincraft.container.EconomyProvider> economy() {
//    return Optional.ofNullable(economyProvider);
//  }
//
//  @Override
//  public JobTaskProvider jobTaskProvider() {
//    return jobTaskProvider;
//  }
//
//  @Override
//  public ConditionFactory conditionFactory() {
//    return BoostFactoryImpl.INSTANCE;
//  }
//
//  @Override
//  public BoostFactory boostFactory() {
//    return BoostFactoryImpl.INSTANCE;
//  }
//
//  @Override
//  public PolicyFactory policyFactory() {
//    return BoostFactoryImpl.INSTANCE;
//  }
//
//  @Override
//  public MobDamageTracker mobDamageTracker() {
//    return mobDamageTracker;
//  }
//
//  @Override
//  public TimedBoostDataService timedBoostDataService() {
//    return timedBoostDataService;
//  }
//}
