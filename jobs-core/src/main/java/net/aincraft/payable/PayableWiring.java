package net.aincraft.payable;

import dev.mintychochip.mint.Mint;
import dev.mintychochip.mint.preferences.PreferenceService;
import dev.mintychochip.mint.preferences.types.EnumPreferenceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import net.aincraft.container.Currency;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarController;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarFormatter;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableType;
import net.aincraft.registry.Registry;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Manual composition for payable types and experience bar (replaces Guice PayableModule).
 */
public final class PayableWiring {

  private static final String ECONOMY_TYPE = "modularjobs:economy";
  private static final String EXPERIENCE_TYPE = "modularjobs:experience";
  private static final Logger LOGGER = Logger.getLogger(PayableWiring.class.getName());

  public final @Nullable EconomyProvider economyProvider;

  private PayableWiring(@Nullable EconomyProvider economyProvider) {
    this.economyProvider = economyProvider;
  }

  public static PayableWiring create(
      Plugin plugin, JobService jobService, Registry<PayableType> payableTypeRegistry) {
    ExperienceBarColorProvider fallback = new DefaultExperienceBarColorProvider();
    ExperienceBarColorPreference colorPreference = new ExperienceBarColorPreference();
    ExperienceBarColorProvider colorProvider =
        new PreferenceExperienceBarColorProvider(colorPreference, fallback);

    if (Mint.PREFERENCE_SERVICE.isLoaded()) {
      PreferenceService service = Mint.PREFERENCE_SERVICE.get();
      service.registerType(new EnumPreferenceType<>(Color.class));
      service.register(colorPreference);
    }

    ExperienceBarController controller = new ExperienceBarControllerImpl(plugin);
    ExperienceBarFormatter formatter = new ExperienceBarFormatterImpl(colorProvider);
    PayableHandler experienceHandler =
        new BufferedExperienceHandlerImpl(controller, formatter, jobService);

    EconomyProvider economyProvider = createEconomyProvider();
    PayableHandler economyHandler = context -> {
      if (economyProvider == null) {
        LOGGER.warning("Cannot pay economy - no provider available");
        return;
      }
      economyProvider.deposit(context.player(), context.payable().amount());
    };

    payableTypeRegistry.register(economyType(economyHandler));
    payableTypeRegistry.register(experienceType(experienceHandler));

    return new PayableWiring(economyProvider);
  }

  @Nullable
  private static EconomyProvider createEconomyProvider() {
    org.bukkit.plugin.Plugin mint = Bukkit.getPluginManager().getPlugin("Mint");
    if (mint != null && mint.isEnabled() && Mint.ECONOMY_SERVICE.isLoaded()) {
      return new MintEconomyProviderImpl(Mint.ECONOMY_SERVICE);
    }
    LOGGER.warning("No economy provider available - Mint not found or not enabled");
    return null;
  }

  private static PayableType economyType(PayableHandler handler) {
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

  private static PayableType experienceType(PayableHandler handler) {
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
}
