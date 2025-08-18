//package net.aincraft.internal;
//
//import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
//import java.math.BigDecimal;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//import net.aincraft.container.BoostSource;
//import net.aincraft.hooks.McMMOBoostSourceImpl;
//import org.bukkit.Bukkit;
//import org.bukkit.plugin.Plugin;
//
//public class BridgeDependencyResolverImpl implements BridgeDependencyResolver {
//
//  private final Plugin plugin;
//
//  public BridgeDependencyResolverImpl(Plugin plugin) {
//    this.plugin = plugin;
//  }
//
//
//  @Override
//  public Optional<BoostSource> getMcMMOBoostSource() {
//    Plugin mcMMO = Bukkit.getPluginManager().getPlugin("McMMO");
//    if (mcMMO != null && mcMMO.isEnabled()) {
//      Map<SuperAbilityType,BigDecimal> amounts = new HashMap<>();
//      amounts.put(SuperAbilityType.SUPER_BREAKER,BigDecimal.valueOf(10000));
//      return Optional.of(McMMOBoostSourceImpl.create(plugin, amounts));
//    }
//    return Optional.empty();
//  }
//}
