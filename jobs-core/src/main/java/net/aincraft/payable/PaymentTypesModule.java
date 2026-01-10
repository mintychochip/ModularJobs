package net.aincraft.payable;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Exposed;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import dev.mintychochip.mint.Mint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarController;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarFormatter;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableType;
import net.aincraft.container.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Consolidated module for all payable type implementations.
 * Uses functional interfaces for economy handler and PayableType renderers.
 */
public final class PaymentTypesModule extends PrivateModule {

  private static final String ECONOMY_TYPE = "modularjobs:economy";
  private static final String EXPERIENCE_TYPE = "modularjobs:experience";
  private static final Logger LOGGER = Logger.getLogger(PaymentTypesModule.class.getName());

  private final Plugin plugin;

  public PaymentTypesModule(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  protected void configure() {
    // Experience handler controller and formatter (kept as classes, too complex for lambdas)
    bind(ExperienceBarController.class).to(ExperienceBarControllerImpl.class).in(Singleton.class);
    bind(ExperienceBarFormatter.class).to(ExperienceBarFormatterImpl.class).in(Singleton.class);
    bind(PayableHandler.class)
        .annotatedWith(Names.named(EXPERIENCE_TYPE))
        .to(BufferedExperienceHandlerImpl.class)
        .in(Singleton.class);
  }

  // ========== Economy Provider ==========

  @Provides
  @Singleton
  @Exposed
  @Nullable
  EconomyProvider economyProvider() {
    PluginManagerProxy pm = new PluginManagerProxy(Bukkit.getPluginManager());
    if (pm.mintAvailable()) {
      return new MintEconomyProviderImpl(Mint.ECONOMY_SERVICE);
    }
    LOGGER.warning("No economy provider available - Mint not found or not enabled");
    return null;
  }

  // ========== Economy Handler (functional) ==========

  @Provides
  @Singleton
  @Exposed
  @Named(ECONOMY_TYPE)
  PayableHandler economyHandler(@Nullable EconomyProvider provider) {
    return context -> {
      if (provider == null) {
        LOGGER.warning("Cannot pay economy - no provider available");
        return;
      }
      provider.deposit(context.player(), context.payable().amount());
    };
  }

  // ========== Payable Type Providers ==========

  @Provides
  @Singleton
  @Exposed
  @Named(ECONOMY_TYPE)
  PayableType economyType(@Named(ECONOMY_TYPE) PayableHandler handler) {
    Key key = NamespacedKey.fromString(ECONOMY_TYPE);
    return new PayableType() {
      private static final String FORMAT = "<#7ed278><symbol><amount></#7ed278>";

      @Override
      public PayableHandler handler() {
        return handler;
      }

      @Override
      public Key key() {
        return key;
      }

      @Override
      public Component render(PayableAmount amount, int places) {
        String symbol = amount.currency().orElse(Currency.USD).symbol();
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(places);
        nf.setMaximumFractionDigits(places);
        BigDecimal value = amount.value().setScale(places, RoundingMode.HALF_UP);
        return MiniMessage.miniMessage().deserialize(FORMAT, TagResolver.builder()
            .tag("symbol", Tag.inserting(Component.text(symbol)))
            .tag("amount", Tag.inserting(Component.text(nf.format(value))))
            .build());
      }
    };
  }

  @Provides
  @Singleton
  @Exposed
  @Named(EXPERIENCE_TYPE)
  PayableType experienceType(@Named(EXPERIENCE_TYPE) PayableHandler handler) {
    Key key = NamespacedKey.fromString(EXPERIENCE_TYPE);
    return new PayableType() {
      private static final String FORMAT = "<#dac65c><amount>xp</#dac65c>";

      @Override
      public PayableHandler handler() {
        return handler;
      }

      @Override
      public Key key() {
        return key;
      }

      @Override
      public Component render(PayableAmount amount, int places) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(places);
        nf.setMaximumFractionDigits(places);
        BigDecimal value = amount.value().setScale(places, RoundingMode.HALF_UP);
        return MiniMessage.miniMessage().deserialize(FORMAT, TagResolver.builder()
            .tag("amount", Tag.inserting(Component.text(nf.format(value))))
            .build());
      }
    };
  }

  /** Proxy to avoid direct Bukkit dependency in test scenarios. */
  private static final class PluginManagerProxy {
    private final org.bukkit.plugin.PluginManager pm;

    PluginManagerProxy(org.bukkit.plugin.PluginManager pm) {
      this.pm = pm;
    }

    boolean mintAvailable() {
      Plugin mint = pm.getPlugin("Mint");
      return mint != null && mint.isEnabled() && Mint.ECONOMY_SERVICE.isLoaded();
    }
  }
}
